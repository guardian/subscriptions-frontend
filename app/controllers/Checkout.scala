package controllers

import actions.CommonActions._
import com.gu.i18n.Currency._
import com.gu.i18n._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.BillingPeriod.SixWeeks
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{NewUsers, NormalisedPromoCode, PromoCode}
import com.gu.memsub.subsv2.CatalogPlan
import com.gu.memsub.{Product, SupplierCode}
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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.applicative._
import scalaz.{NonEmptyList, OptionT}
object Checkout extends Controller with LazyLogging with CatalogProvider {

  import SessionKeys.{Currency => _, UserId => _, _}

  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def getBetterPlans[A <: CatalogPlan.Paid](plan: A, others: List[A]) =
    others.sortBy(_.charges.gbpPrice.amount).dropWhile(_.charges.gbpPrice.amount <= plan.charges.gbpPrice.amount)

  def renderCheckout(countryGroup: String, promoCode: Option[PromoCode], supplierCode: Option[SupplierCode], forThisPlan: String) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    // countryGroup String above basically means now 'countryOrCountryGroup' so we'll use the fromHint API on DetermineCountryGroup
    val determinedCountryGroup = DetermineCountryGroup.fromHint(countryGroup) getOrElse CountryGroup.UK

    val matchingPlanList: Option[PlanList[CatalogPlan.ContentSubscription]] = {

      val testOnlyPlans = if (tpBackend == TouchpointBackend.Test) List(catalog.weeklyZoneB.toList.filter(_.availableForCheckout)) else List.empty

      val contentSubscriptionPlans = List(
        catalog.delivery.list,
        catalog.voucher.list,
        catalog.weeklyZoneA.toList.filter(_.availableForCheckout),
        catalog.weeklyZoneC.toList,
        catalog.digipack.toList) ++ testOnlyPlans

      def matchPlan(planCandidates: List[CatalogPlan.ContentSubscription]) = planCandidates.find(_.slug == forThisPlan).map(p => PlanList(p, getBetterPlans(p, planCandidates): _*))

      contentSubscriptionPlans.toIterator.map(matchPlan).find(_.isDefined).flatten
    }

    val idUser = (for {
      authUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
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
          case Product.Digipack => getSettings(determinedCountryGroup.defaultCountry, GBP)
          case Product.Delivery => getSettings(Some(Country.UK), GBP)
          case Product.Voucher => getSettings(Some(Country.UK), GBP)
          case Product.WeeklyZoneA => getSettings(determinedCountryGroup.defaultCountry, GBP)
          case Product.WeeklyZoneB => getSettings(None, USD)
          case Product.WeeklyZoneC => getSettings(None, USD)
        }

        val digitalEdition = model.DigitalEdition.getForCountry(countryAndCurrencySettings.defaultCountry)

        // either a code to send to the form (left) or a tracking code for the session (right)
        val countryToValidatePromotionAgainst = countryAndCurrencySettings.defaultCountry orElse determinedCountryGroup.defaultCountry
        val validatedPromoCode: Either[PromoCode, Seq[(String, String)]] = (promoCode |@| countryToValidatePromotionAgainst) (
          tpBackend.promoService.validate[NewUsers](_: PromoCode, _: Country, planList.default.id)
        ).flatMap(_.toOption).map(vp => vp.promotion.asTracking.map(_ => Seq(PromotionTrackingCode -> vp.code.get)).toRight(vp.code))
          .getOrElse(Right(Seq.empty))

        val resolvedSupplierCode = supplierCode orElse request.session.get(SupplierTrackingCode).map(SupplierCode) // query param wins
        val trackingCodeSessionData = validatedPromoCode.right.toSeq.flatten
        val supplierCodeSessionData = resolvedSupplierCode.map(code => Seq(SupplierTrackingCode -> code.get)).getOrElse(Seq.empty)
        val productData = ProductPopulationData(user.map(_.address), planList)
        val promoCodeExists = promoCode.filter(tpBackend.promoService.findPromotion(_).isDefined)

        Ok(views.html.checkout.payment(
          personalData = personalData,
          productData = productData,
          countryGroup = determinedCountryGroup,
          touchpointBackendResolution = resolution,
          promoCode = validatedPromoCode.left.toOption orElse promoCodeExists,
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

  }

  def handleCheckout = NoCacheAction.async { implicit request =>

    //there's an annoying circular dependency going on here
    val tempData = SubscriptionsForm.subsForm.bindFromRequest().value
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(NameEnteredInForm, tempData)
    implicit val tpBackend = resolution.backend
    val idUserOpt = authenticatedUserFor(request)
    val srEither = tpBackend.subsForm.bindFromRequest

    val sr = srEither.valueOr {
      e => throw new Exception(s"Backend validation failed: identityId=${idUserOpt.map(_.user.id).mkString};" +
        s" JavaScriptEnabled=${request.headers.toMap.contains("X-Requested-With")};" +
        s" ${e.map(err => s"${err.key} ${err.message}").mkString(", ")}")
    }
    val sessionTrackingCode = request.session.get(PromotionTrackingCode)
    val sessionSupplierCode = request.session.get(SupplierTrackingCode)
    val subscribeRequest = sr.copy(
      genericData = sr.genericData.copy(
        promoCode = sr.genericData.promoCode orElse sessionTrackingCode.map(PromoCode)
      )
    )

    val requestData = SubscriptionRequestData(
      ipAddress = ProxiedIP.getIP(request),
      ipCountry = request.getFastlyCountry,
      supplierCode = sessionSupplierCode.map(SupplierCode)
    )

    val checkoutResult = checkoutService.processSubscription(subscribeRequest, idUserOpt, requestData)

    def failure(seqErr: NonEmptyList[SubsError]) = {
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
        SessionKeys.Currency -> subscribeRequest.genericData.currency.toString
      )

      val userSessionFields = r.userIdData match {
        case Some(GuestUser(UserId(userId), IdentityToken(token))) =>
          Seq(SessionKeys.UserId -> userId, IdentityGuestPasswordSettingToken -> token)
        case _ => Seq()
      }

      val appliedCode = r.validPromotion.flatMap(vp => vp.promotion.asTracking.fold[Option[PromoCode]](Some(vp.code))(_ => None))
      val appliedCodeSession = appliedCode.map(promoCode => Seq(AppliedPromoCode -> promoCode.get)).getOrElse(Seq.empty)
      val subscriptionDetails = Some(StartDate -> subscribeRequest.productData.fold(_.startDate, _ => LocalDate.now).toString("d MMMM YYYY"))

      // Don't remove the SupplierTrackingCode from the session
      val session = (productData ++ userSessionFields ++ appliedCodeSession ++ subscriptionDetails).foldLeft(request.session - AppliedPromoCode - PromotionTrackingCode) {
        _ + _
      }

      logger.info(s"User successfully became subscriber:\n\tUser: SF=${r.salesforceMember.salesforceContactId}\n\tPlan: ${subscribeRequest.productData.fold(_.plan, _.plan).name}\n\tSubscription: ${r.subscribeResult.subscriptionName}")
      Ok(Json.obj("redirect" -> routes.Checkout.thankYou().url)).withSession(session)
    }

    checkoutResult.map(_.fold(failure, success))
  }

  def convertGuestUser = NoCacheAction.async(parse.form(FinishAccountForm())) { implicit request =>
    val guestAccountData = request.body
    IdentityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token))
      .map { cookies =>
        Ok(Json.obj("profileUrl" -> webAppProfileUrl.toString())).withCookies(cookies: _*)
      }
  }

  def thankYou() = NoCacheAction { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SubsName)
      plan <- session.get(RatePlanId).map(ProductRatePlanId).flatMap(p => catalog.allSubs.flatten.find(_.id == p))
      ratePlanId <- session.get(RatePlanId)
      currencyStr <- session.get(SessionKeys.Currency)
      currency <- Currency.fromString(currencyStr)
      startDate <- session.get(StartDate)
    } yield (subsName, plan, currency, startDate)
    sessionInfo.fold {
      Redirect(routes.Homepage.index()).withNewSession
    } { case (subsName, plan, currency, startDate) =>

      val passwordForm = authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.UserId)
          token <- session.get(IdentityGuestPasswordSettingToken)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      } {
        const(None)
      } // Don't display the user registration form if the user is logged in

      val promotion = session.get(AppliedPromoCode).flatMap(code => resolution.backend.promoService.findPromotion(NormalisedPromoCode.safeFromString(code)))
      Ok(view.thankyou(subsName, passwordForm, resolution, plan, promotion, currency, startDate))
    }

  }

  def checkIdentity(email: String) = CachedAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  val parseDirectDebitForm: BodyParser[DirectDebitData] = parse.form[DirectDebitData](Form(SubscriptionsForm.directDebitDataMapping))

  def checkAccount = CachedAction.async(parseDirectDebitForm) { implicit request =>
    for {
      isAccountValid <- GoCardlessService.checkBankDetails(request.body)
    } yield Ok(Json.obj("accountValid" -> isAccountValid))
  }

  def findAddress(postCode: String) = CSRFCachedAsyncAction { implicit request =>
    GetAddressIOService.find(postCode).map { result =>
      // Capital A 'Addresses' is for compatibility with the https://api.getaddress.io/v2/uk/ response,
      // should a client want not to proxy via this server.
      Ok(Json.obj("Addresses" -> result.Addresses))
    } recover {
      case e => BadRequest(Json.obj("Message" -> e.getMessage))
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
      case _ => handleError("UknownPaymentError")
    }
  }
}
