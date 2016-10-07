package controllers
import java.text.NumberFormat.Field

import actions.CommonActions._
import com.gu.i18n._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.Product.Delivery
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion.{AnyPromotion, _}
import com.gu.memsub.promo.{NewUsers, PromoCode}
import com.gu.memsub.subsv2.CatalogPlan._
import com.gu.memsub.subsv2.{CatalogPlan, PaidChargeList}
import com.gu.memsub.{BillingPeriod, Product, SupplierCode}
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
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
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{BillingPeriod => _, _}

import scalaz.std.option._
import scalaz.syntax.applicative._
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

  def renderCheckout(countryGroup: CountryGroup, promoCode: Option[PromoCode], supplierCode: Option[SupplierCode], forThisPlan: String) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val idUser = (for {
      authUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
    } yield idUser).run

    idUser map { user =>

      val planListEither: Either[PlanList[CatalogPlan.Digipack[BillingPeriod]], PlanList[CatalogPlan.Paper]] = (
        catalog.delivery.list.find(_.slug == forThisPlan).map(p => PlanList[CatalogPlan.Delivery](p, getBetterPlans(p, catalog.delivery.list):_*)) orElse
        catalog.voucher.list.find(_.slug == forThisPlan).map(p => PlanList[Voucher](p, getBetterPlans(p, catalog.voucher.list):_*)) orElse
        catalog.weeklyUK.toList.find(_.slug == forThisPlan).map(p => PlanList[WeeklyUK[BillingPeriod]](p, getBetterPlans(p, catalog.weeklyUK.toList):_*)) orElse
        catalog.weeklyROW.toList.find(_.slug == forThisPlan).map(p => PlanList[WeeklyROW[BillingPeriod]](p, getBetterPlans(p, catalog.weeklyROW.toList):_*))
        ).toRight(PlanList(catalog.digipack.month, catalog.digipack.quarter, catalog.digipack.year))

      val plans = planListEither.fold(identity, identity)
      val supportedCurrencies = plans.default.charges.price.currencies
      val personalData = user.map(PersonalData.fromIdUser)

      val identityCountry = personalData.flatMap(data => data.address.country)

      object CountryAndCurrencySettings {
        def withIdDefaultsOrFallback(options: List[CountryWithCurrency], fallbackCountry: Option[Country], fallbackCurrency: Currency):CountryAndCurrencySettings = {

          def selectedIdDefault = identityCountry.flatMap(c => options.find(_.country == c))
          def selectedFallbackDefault = fallbackCountry.flatMap { c => options.find(_.country == c) }
          val selectedDefault = selectedIdDefault orElse selectedFallbackDefault

          CountryAndCurrencySettings(
            options = options,
            defaultCountry = selectedDefault.map(_.country),
            defaultCurrency = selectedDefault.map(_.currency).getOrElse(fallbackCurrency)
          )
        }
      }
      case class CountryAndCurrencySettings(options: List[CountryWithCurrency], defaultCountry: Option[Country], defaultCurrency: Currency)


      val ukAndIsleOfMan = CountryGroup.UK.copy(countries = List(Country.UK, Country("IM", "Isle of Man")))

      val rowUk = CountryGroup("Row Uk", "uk", None, CountryGroup.UK.countries.filterNot(ukAndIsleOfMan.countries.contains(_)), GBP, PostCode)

      val weeklyUkNorthAmericaGroups = List (ukAndIsleOfMan, CountryGroup.US, CountryGroup.Canada)

      val weeklyRowGroups = rowUk :: CountryGroup.allGroups.filterNot(group => (CountryGroup.UK :: weeklyUkNorthAmericaGroups) contains group)

      val deliverySettings = CountryAndCurrencySettings.withIdDefaultsOrFallback(
        options = List(CountryWithCurrency(Country.UK, GBP)),
        fallbackCountry = Some(Country.UK),
        fallbackCurrency = GBP
      )

      val countryAndCurrencySettings = plans.default.product match {

        case Product.Digipack => CountryAndCurrencySettings.withIdDefaultsOrFallback(
          options = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
          fallbackCountry = countryGroup.defaultCountry,
          fallbackCurrency = GBP
        )
        case Product.Voucher => deliverySettings.copy(options = CountryWithCurrency.fromCountryGroup(ukAndIsleOfMan))
        case Product.Delivery => deliverySettings
        case Product.WeeklyUK => CountryAndCurrencySettings.withIdDefaultsOrFallback(
          options = CountryWithCurrency.whitelisted(supportedCurrencies, GBP, weeklyUkNorthAmericaGroups),
          fallbackCountry = Some(Country.UK),
          fallbackCurrency = GBP
        )
        case Product.WeeklyROW => {
          CountryAndCurrencySettings.withIdDefaultsOrFallback(
            options = CountryWithCurrency.whitelisted(supportedCurrencies, USD, weeklyRowGroups),
            fallbackCountry = None,
            fallbackCurrency = USD
          )
        }
        //case _ => TODO do something to match all cases?
      }

      val defaultCountry = countryAndCurrencySettings.defaultCountry
      val digitalEdition = model.DigitalEdition.getForCountry(defaultCountry)

      // either a code to send to the form (left) or a tracking code for the session (right)
      val promo: Either[PromoCode, Seq[(String, String)]] = (promoCode |@| defaultCountry)(
        tpBackend.promoService.validate[NewUsers](_: PromoCode, _: Country, plans.default.id)
      ).flatMap(_.toOption).map(vp => vp.promotion.asTracking.map(_ => Seq(PromotionTrackingCode -> vp.code.get)).toRight(vp.code))
       .getOrElse(Right(Seq.empty))

      val resolvedSupplierCode = supplierCode orElse request.session.get(SupplierTrackingCode).map(SupplierCode) // query param wins
      val trackingCodeSessionData = promo.right.toSeq.flatten
      val supplierCodeSessionData = resolvedSupplierCode.map(code => Seq(SupplierTrackingCode -> code.get)).getOrElse(Seq.empty)
      val productData = ProductPopulationData(user.map(_.address), planListEither)
      Ok(views.html.checkout.payment(
        personalData = personalData,
        productData = productData,
        country = countryAndCurrencySettings.defaultCountry,
        countryGroup = countryGroup,
        defaultCurrency = countryAndCurrencySettings.defaultCurrency,
        countriesWithCurrency = countryAndCurrencySettings.options,
        touchpointBackendResolution = resolution,
        promoCode = promo.left.toOption,
        supplierCode = resolvedSupplierCode,
        edition = digitalEdition
      )).withSession(trackingCodeSessionData ++ supplierCodeSessionData:_*)
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

    val requestData = SubscriptionRequestData(ProxiedIP.getIP(request), supplierCode = sessionSupplierCode.map(SupplierCode))
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

      val promotion = session.get(AppliedPromoCode).flatMap(code => resolution.backend.promoService.findPromotion(PromoCode(code)))
      Ok(view.thankyou(subsName, passwordForm, resolution, plan, promotion, currency, startDate))
    }

  }

  def validatePromoCode(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country) = NoCacheAction { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    import com.gu.memsub.Subscription.ProductRatePlanId
    import views.support.Pricing._

    def getAdjustedRatePlans(promo: AnyPromotion): Option[Map[String, String]] = {
      case class RatePlanPrice(ratePlanId: ProductRatePlanId, chargeList: PaidChargeList)
      promo.asDiscount.map { discountPromo =>
        catalog.allSubs.flatten
          .filter(plan => promo.appliesTo.productRatePlanIds.contains(plan.id))
          .map(plan => RatePlanPrice(plan.id, plan.charges)).map { ratePlanPrice =>
          val currency = CountryGroup.byCountryCode(country.alpha2).getOrElse(CountryGroup.UK).currency
          ratePlanPrice.ratePlanId.get -> ratePlanPrice.chargeList.prettyPricingForDiscountedPeriod(discountPromo, currency)
        }.toMap
      }
    }

    tpBackend.promoService.findPromotion(promoCode).fold {
      NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code."))
    } { promo =>
      val result = promo.validateFor(prpId, country)
      val body = Json.obj(
        "promotion" -> Json.toJson(promo),
        "adjustedRatePlans" -> Json.toJson(getAdjustedRatePlans(promo)),
        "isValid" -> result.isRight,
        "errorMessage" -> result.swap.toOption.map(_.msg)
      )
      result.fold(_ => NotAcceptable(body), _ => Ok(body))
    }
  }

  def validateDelivery(postCode: String) = CachedAction { implicit request =>
    HomeDeliveryPostCodes.findDistrict(postCode).fold(NotAcceptable(""))(pc => Ok(Json.obj("availableDistrict" -> pc)))
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
