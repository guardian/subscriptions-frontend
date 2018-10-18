package controllers

import java.util.UUID

import actions.CommonActions
import cats.data.EitherT
import cats.instances.future._
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.i18n.CountryGroup._
import com.gu.i18n.Currency._
import com.gu.i18n._
import com.gu.memsub.Subscription.Name
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.memsub.subsv2.CatalogPlan.ContentSubscription
import com.gu.memsub.subsv2.{CatalogPlan, PlansWithIntroductory}
import com.gu.memsub.{Product, SupplierCode}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.tip.Tip
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.ContentSubscriptionPlanOps._
import model.IdUserOps._
import model._
import model.error.CheckoutService._
import model.error.SubsError
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services._
import utils.RequestCountry._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{PlanList, BillingPeriod => _, _}

import scala.Function.const
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scalaz.std.scalaFuture._
import scalaz.{NonEmptyList, OptionT}
import utils.TestUsers

class Checkout(fBackendFactory: TouchpointBackends, commonActions: CommonActions, implicit val executionContext: ExecutionContext, override protected val controllerComponents: ControllerComponents) extends BaseController with LazyLogging {

  import SessionKeys.{Currency => _, UserId => _, _}
  import commonActions._

  def checkoutService(implicit res: TouchpointBackends.Resolution): CheckoutService =
    res.backend.checkoutService

  def getBetterPlans[A <: CatalogPlan.Paid](plan: A, allPlans: List[A]) =
    allPlans.sortBy(_.charges.gbpPrice.amount).dropWhile(_.charges.gbpPrice.amount <= plan.charges.gbpPrice.amount)

  def renderCheckout(countryGroup: String, promoCode: Option[PromoCode], supplierCode: Option[SupplierCode], forThisPlan: String) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    // countryGroup String above basically means now 'countryOrCountryGroup' so we'll use the fromHint API on DetermineCountryGroup
    val determinedCountryGroup = DetermineCountryGroup.fromHint(countryGroup) getOrElse UK

    tpBackend.catalogService.catalog.map { _.map { catalog =>

      val matchingPlanList: Option[PlanList[ContentSubscription]] = {

        val testOnlyPlans = if (tpBackend == fBackendFactory.Test) List(catalog.weekly.zoneB.plansWithAssociations) else List.empty

        val productsWithoutIntroductoryPlans = List(
          catalog.delivery.list.toList,
          catalog.voucher.list.toList,
          catalog.digipack.plans
        ).map(plans => PlansWithIntroductory(plans, List.empty))

        val productsWithIntroductoryPlans = List(
          catalog.weekly.zoneA.plansWithAssociations,
          catalog.weekly.zoneC.plansWithAssociations,
          catalog.weekly.domestic.plansWithAssociations,
          catalog.weekly.restOfWorld.plansWithAssociations
        ) ++ testOnlyPlans

        val contentSubscriptionPlans = productsWithoutIntroductoryPlans ++ productsWithIntroductoryPlans
        contentSubscriptionPlans.flatMap {
          case PlansWithIntroductory(plans, associations)
            if (plans.exists(_.slug == forThisPlan)) => {
            val buyablePlans = plans.filter(_.availableForCheckout)
            val plansInPriceOrder = buyablePlans.sortBy(_.charges.gbpPrice.amount)
            val selectedPlan = plansInPriceOrder.find(_.slug == forThisPlan)
            selectedPlan.map(PlanList(associations,_,plansInPriceOrder))
          }
          case _ => None
        }.headOption
      }

      val idUser = (for {
        authUser <- OptionT(Future.successful(authenticatedUserFor(request)))
        idUser <- OptionT(fBackendFactory.identityService.userLookupByCredentials(authUser.credentials))
      } yield idUser).run

      idUser map { user =>

        def renderCheckoutFor(planList: PlanList[CatalogPlan.ContentSubscription]) = {

          val personalData = user.map(PersonalData.fromIdUser)

          def getSettings(fallbackCountry: Option[Country], fallbackCurrency: Currency) = {
            val localizationSettings = planList.default.localizationSettings
            val availableCountries = localizationSettings.availableDeliveryCountriesWithCurrency.getOrElse(localizationSettings.availableBillingCountriesWithCurrency)

            def availableCountryWithCurrencyFor(country: Option[Country]) = country.flatMap(c => availableCountries.find(_.country == c))
            val personalDataCountry = personalData.flatMap(data => data.address.country)

            val defaultCountryWithCurrency = availableCountryWithCurrencyFor(personalDataCountry) orElse availableCountryWithCurrencyFor(fallbackCountry)

            CountryAndCurrencySettings(
              availableDeliveryCountries = localizationSettings.availableDeliveryCountriesWithCurrency,
              availableBillingCountries = localizationSettings.availableBillingCountriesWithCurrency,
              defaultCountry = defaultCountryWithCurrency.map(_.country),
              defaultCurrency = defaultCountryWithCurrency.map(_.currency).getOrElse(fallbackCurrency)
            )
          }

          val countryAndCurrencySettings = planList.default.product match {
            case Product.Digipack => getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
            case Product.Delivery => getSettings(UK.defaultCountry, UK.currency)
            case Product.Voucher => getSettings(UK.defaultCountry, UK.currency)
            case Product.WeeklyZoneA => {
              if (GuardianWeeklyZones.zoneACountryGroups.contains(determinedCountryGroup)) {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              } else {
                getSettings(UK.defaultCountry, UK.currency)
              }
            }
            case Product.WeeklyZoneB | Product.WeeklyZoneC => {
              if (GuardianWeeklyZones.zoneACountryGroups.contains(determinedCountryGroup)) {
                getSettings(None, USD)

              } else {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              }
            }

            case Product.WeeklyDomestic => {
              if (GuardianWeeklyZones.domesticZoneCountryGroups.contains(determinedCountryGroup)) {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              } else {
                getSettings(UK.defaultCountry, UK.currency)
              }
            }
            case Product.WeeklyRestOfWorld => {
              if (GuardianWeeklyZones.domesticZoneCountryGroups.contains(determinedCountryGroup)) {
                getSettings(None, USD)
              } else {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              }
            }
          }

          val digitalEdition = model.DigitalEdition.getForCountry(countryAndCurrencySettings.defaultCountry)

          // either a code to send to the form (left) or a tracking code for the session (right)
          val countryToValidatePromotionAgainst = countryAndCurrencySettings.defaultCountry orElse determinedCountryGroup.defaultCountry

          val promotion = promoCode.flatMap(tpBackend.promoService.findPromotion)

          val trackingPromotion = promotion.flatMap(_.asTracking)
          val displayPromoCode = if(promotion.isDefined && trackingPromotion.isEmpty) promoCode else None
          val trackingCodeSessionData: Seq[(String, String)] = trackingPromotion.flatMap(_ => promoCode.map(p => (PromotionTrackingCode -> p.get))).toSeq

          val resolvedSupplierCode = supplierCode orElse request.session.get(SupplierTrackingCode).map(SupplierCode) // query param wins
          val supplierCodeSessionData = resolvedSupplierCode.map(code => Seq(SupplierTrackingCode -> code.get)).getOrElse(Seq.empty)
          val productData = ProductPopulationData(user.map(_.address), planList)
          Ok(views.html.checkout.payment(
            personalData = personalData,
            productData = productData,
            countryGroup = determinedCountryGroup,
            touchpointBackendResolution = resolution,
            promoCode = displayPromoCode,
            supplierCode = resolvedSupplierCode,
            edition = digitalEdition,
            countryAndCurrencySettings = countryAndCurrencySettings
          )).withSession(request.session.data.toSeq ++ trackingCodeSessionData ++ supplierCodeSessionData: _*)
        }

        matchingPlanList match {
          case Some(planList) => renderCheckoutFor(planList)
          case None => Redirect(routes.Homepage.index())
        }
      }
    }.valueOr { err =>
      SafeLogger.error(scrub"failed renderCheckout: ${err.list.toList.mkString(", ")}")
      Future.successful(InternalServerError("failed to render checkout due to catalog issues"))
    }
    }.flatMap(identity)
  }

  def handleCheckout = NoCacheAction.async { implicit request =>

    //there's an annoying circular dependency going on here
    val tempData = SubscriptionsForm.subsForm.bindFromRequest().value
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(NameEnteredInForm, tempData)
    implicit val tpBackend = resolution.backend
    val idUserOpt = authenticatedUserFor(request)

    val sessionTrackingCode = request.session.get(PromotionTrackingCode)
    val sessionSupplierCode = request.session.get(SupplierTrackingCode)

    tpBackend.subsForm.flatMap { subsForm =>

      val preliminarySubscribeRequest = {
        subsForm.bindFromRequest.valueOr {
          e =>
            throw new Exception(scrub"Backend validation failed: identityId=${idUserOpt.map(_.user.id).mkString};" +
              s" JavaScriptEnabled=${request.headers.toMap.contains("X-Requested-With")};" +
              s" ${e.map(err => s"${err.key} ${err.message}").mkString(", ")}")
        }
      }
      val subscribeRequest = preliminarySubscribeRequest.copy(
        genericData = preliminarySubscribeRequest.genericData.copy(
          promoCode = preliminarySubscribeRequest.genericData.promoCode orElse sessionTrackingCode.map(PromoCode)
        )
      )

    val requestData = SubscriptionRequestData(
      ipCountry = request.getFastlyCountry,
      supplierCode = sessionSupplierCode.map(SupplierCode)
    )

    val checkoutResult = checkoutService.processSubscription(subscribeRequest, idUserOpt, requestData)

    def failure(seqErr: NonEmptyList[SubsError]) = {
      Future.successful(
        seqErr.head match {
          case e: CheckoutIdentityFailure =>
            logger.error(SubsError.header(e))
            logger.warn(SubsError.toStringPretty(e))

            Forbidden(Json.obj("type" -> "CheckoutIdentityFailure",
              "message" -> "User could not subscribe because Identity Service could not register the user"))

          case e: CheckoutStripeError =>
            logger.warn(SubsError.toStringPretty(e))

            Forbidden(Json.obj(
              "type" -> "CheckoutStripeError",
              "message" -> e.paymentError.getMessage))

          case e: CheckoutZuoraPaymentGatewayError =>
            logger.warn(SubsError.toStringPretty(e))
            handlePaymentGatewayError(e.paymentError, e.purchaserIds, subscribeRequest.genericData.personalData.address.countryName)

          case e: CheckoutPaymentTypeFailure =>
            logger.error(SubsError.header(e))
            logger.warn(SubsError.toStringPretty(e))

            Forbidden(Json.obj("type" -> "CheckoutPaymentTypeFailure",
              "message" -> e.msg))

          case e: CheckoutSalesforceFailure =>
            logger.error(SubsError.header(e))
            logger.warn(SubsError.toStringPretty(e))

            Forbidden(Json.obj("type" -> "CheckoutSalesforceFailure",
              "message" -> e.msg))

          case e: CheckoutGenericFailure =>
            logger.error(SubsError.header(e))
            logger.warn(SubsError.toStringPretty(e))

            Forbidden(Json.obj("type" -> "CheckoutGenericFailure",
              "message" -> "User could not subscribe"))
        }
      )
    }

    def success(r: CheckoutSuccess) = {

      r.nonFatalErrors.map(e => SubsError.header(e) -> SubsError.toStringPretty(e)).foreach {
        case (h, m) =>
          logger.warn(h)
          logger.error(m)
      }
      val productData = Seq(
        SubsName -> r.subscribeResult.subscriptionName,
        RatePlanId -> subscribeRequest.productRatePlanId.get,
        SessionKeys.Currency -> subscribeRequest.genericData.currency.toString,
        BillingCountryName -> subscribeRequest.genericData.personalData.address.countryName
      )

      val userSessionFields = r.userIdData match {
        case Some(GuestUser(UserId(userId), IdentityToken(token))) =>
          Seq(SessionKeys.UserId -> userId, IdentityGuestPasswordSettingToken -> token)
        case _ => Seq()
      }

      val appliedCode = r.validPromotion.flatMap(vp => vp.promotion.asTracking.fold[Option[PromoCode]](Some(vp.code))(_ => None))
      val appliedCodeSession = appliedCode.map(promoCode => Seq(AppliedPromoCode -> promoCode.get)).getOrElse(Seq.empty)
      val subscriptionDetails = Some(StartDate -> subscribeRequest.productData.fold(_.startDate, _ => LocalDate.now).toString("d MMMM YYYY"))
      val marketingOptIn = Seq(MarketingOptIn -> subscribeRequest.genericData.personalData.receiveGnmMarketing.toString)
      // Don't remove the SupplierTrackingCode from the session
      val session = (productData ++ userSessionFields ++ marketingOptIn ++ appliedCodeSession ++ subscriptionDetails).foldLeft(request.session - AppliedPromoCode - PromotionTrackingCode) {
        _ + _
      }

      submitAcquisitionEvent(request, subscribeRequest).value.map { _ =>
        logger.info(s"User successfully became subscriber:\n\tUser: SF=${r.salesforceMember.salesforceContactId}\n\tPlan: ${subscribeRequest.productData.fold(_.plan, _.plan).name}\n\tSubscription: ${r.subscribeResult.subscriptionName}")
        Ok(Json.obj("redirect" -> routes.Checkout.thankYou().url)).withSession(session)
      }
    }

    def submitAcquisitionEvent(request: Request[AnyContent], subscribeRequest: SubscribeRequest): EitherT[Future, Unit, AcquisitionSubmission] = {
      val testUser = TestUsers.isTestUser(PreSigninTestCookie, request.cookies)(request)
      if(testUser.isEmpty) {
        val acquisitionData = request.session.get("acquisitionData")
        if (acquisitionData.isEmpty) {
          logger.warn(s"No acquisitionData in session")
        }

        val promotion = subscribeRequest.genericData.promoCode.map(_.get).flatMap(code => tpBackend.promoService.findPromotion(NormalisedPromoCode.safeFromString(code)))
        val clientBrowserInfo = ClientBrowserInfo(
          request.cookies.get("_ga").map(_.value).getOrElse(UUID.randomUUID().toString), //Get GA client id from cookie
          request.headers.get("user-agent"),
          request.remoteAddress
        )

        //This service is mocked unless it's running in PROD, change to test acquisition events are working
        AcquisitionService(isTestService = tpBackend.environmentName != "PROD")
          .submit(SubscriptionAcquisitionComponents(subscribeRequest, promotion, acquisitionData, clientBrowserInfo))
          .leftMap(
            err => logger.warn(s"Error submitting acquisition data. $err")
          )
      } else {
        val either: Future[Either[Unit, AcquisitionSubmission]] = Future.successful(Left(Unit))
        EitherT(either) // Don't submit acquisitions for test users
      }
    }

    checkoutResult.flatMap(_.fold(failure, success))

    }

  }

  def convertGuestUser = NoCacheAction.async(parse.form(FinishAccountForm())) { implicit request =>
    val guestAccountData = request.body
    val marketingOptIn = Try(request.session.get(MarketingOptIn).getOrElse("false").toBoolean).getOrElse(false)

    fBackendFactory.identityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token), marketingOptIn)
      .map { cookies =>
        Ok(Json.obj("profileUrl" -> webAppProfileUrl.toString())).withCookies(cookies: _*)
      }
  }

  def thankYou() = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SubsName)
      startDate <- session.get(StartDate)
    } yield (subsName, startDate)
    sessionInfo.fold {
      Future.successful(Redirect(routes.Homepage.index()).withNewSession)
    } { case (subsName, startDate) =>

      val passwordForm = authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.UserId)
          token <- session.get(IdentityGuestPasswordSettingToken)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      } {
        const(None)
      } // Don't display the user registration form if the user is logged in

      val promoCode = session.get(AppliedPromoCode)
      val promotion = promoCode.flatMap(code => resolution.backend.promoService.findPromotion(NormalisedPromoCode.safeFromString(code)))

      val billingCountry = session.get(BillingCountryName).flatMap(CountryGroup.countryByNameOrCode) orElse request.getFastlyCountry
      val edition: model.DigitalEdition = model.DigitalEdition.getForCountry(billingCountry)
      val eventualMaybeSubscription = tpBackend.subscriptionService.get[com.gu.memsub.subsv2.SubscriptionPlan.ContentSubscription](Name(subsName))
      eventualMaybeSubscription.map { maybeSub =>
        maybeSub.map { sub =>
          if (tpBackend.environmentName == "PROD") Tip.verify()
          Ok(view.thankyou(sub, passwordForm, resolution, promoCode, promotion, startDate, edition, billingCountry))
        }.getOrElse {
          Redirect(routes.Homepage.index()).withNewSession
        }
      }
    }

  }

  def checkIdentity(email: String) = NoCacheAction.async { implicit request =>
    for {
      doesUserExist <- fBackendFactory.identityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  val parseDirectDebitForm: BodyParser[DirectDebitData] = parse.form[DirectDebitData](Form(SubscriptionsForm.directDebitDataMapping))

  def checkAccount = NoCacheAction.async(parseDirectDebitForm) { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    for {
      isAccountValid <- tpBackend.goCardlessService.checkBankDetails(request.body)
    } yield Ok(Json.obj("accountValid" -> isAccountValid))
  }

  def findAddress(postCode: String) = NoCacheAction.async { implicit request =>
    new GetAddressIOService().find(postCode).map { result =>
      // Capital A 'Addresses' is for compatibility with the https://api.getaddress.io/v2/uk/ response,
      // should a client want not to proxy via this server.
      Ok(Json.obj("Addresses" -> result.Addresses))
    } recover {
      case error if error.getMessage == "Bad Request" =>
        BadRequest //The postcode was invalid
      case error =>
        SafeLogger.error(scrub"Failed to complete postcode lookup via getAddress.io due to: $error")
        InternalServerError
    }
  }

  // PaymentGatewayError should be logged at WARN level
  private def handlePaymentGatewayError(e: PaymentGatewayError, purchaserIds: PurchaserIdentifiers, country: String) = {

    def handleError(code: String) = {
      logger.warn(s"User $purchaserIds could not subscribe due to payment gateway failed transaction: \n\terror=${e} \n\tuser=$purchaserIds \n\tcountry=$country")
      Forbidden(Json.obj("type" -> "PaymentGatewayError", "code" -> code))
    }

    e.errType match {
      case Fraudulent => handleError("Fraudulent")
      case TransactionNotAllowed => handleError("TransactionNotAllowed")
      case DoNotHonor => handleError("DoNotHonor")
      case InsufficientFunds => handleError("InsufficientFunds")
      case RevocationOfAuthorization => handleError("RevocationOfAuthorization")
      case GenericDecline => handleError("GenericDecline")
      case _ => handleError("UnknownPaymentError")
    }
  }
}
