package controllers
import actions.CommonActions._
import com.gu.i18n._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.{BillingPeriod, SupplierCode}
import com.gu.memsub.subsv2.{CatalogPlan, Delivery, PaidChargeList}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.{NewUsers, PromoCode}
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.subscriptions.DigipackPlan
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
import scalaz.std.scalaFuture._
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
        catalog.delivery.list.find(_.slug == forThisPlan).map(p => PlanList(p, getBetterPlans(p, catalog.delivery.list):_*)) orElse
        catalog.voucher.list.find(_.slug == forThisPlan).map(p => PlanList(p, getBetterPlans(p, catalog.delivery.list):_*))
      ).toRight(PlanList(catalog.digipack.month, catalog.digipack.quarter, catalog.digipack.year))

      val plans = planListEither.fold(identity, identity)
      val personalData = user.map(PersonalData.fromIdUser)
      val productData = ProductPopulationData(user.map(_.address), planListEither)
      val preselectedCountry = personalData.flatMap(data => data.address.country)
      val countryGroupForPreselectedCountry = preselectedCountry.flatMap(c => CountryGroup.byCountryCode(c.alpha2))
      val determinedCountryGroup = if (planListEither.isRight) {
        val countriesInDropDown = if (plans.default.product == Delivery) List(Country.UK) else List(Country("IM", "Isle of Man"), Country.UK)
        CountryGroup(Country.UK.name, "uk", Some(Country.UK), countriesInDropDown, CountryGroup.UK.currency, PostCode)
      } else {
        countryGroupForPreselectedCountry.getOrElse(countryGroup)
      }
      val determinedCountry = preselectedCountry orElse determinedCountryGroup.defaultCountry

      val supportedCurrencies = plans.default.charges.price.currencies
      val determinedCurrency = if (supportedCurrencies.contains(determinedCountryGroup.currency)) determinedCountryGroup.currency else GBP
      val determinedCountriesWithCurrency = if (planListEither.isRight) {
        determinedCountryGroup.countries.map(c => CountryWithCurrency(c, determinedCurrency))
      } else {
        CountryWithCurrency.whitelisted(supportedCurrencies, GBP)
      }

      // either a code to send to the form (left) or a tracking code for the session (right)
      val promo: Either[PromoCode, Seq[(String, String)]] = (promoCode |@| determinedCountry)(
        tpBackend.promoService.validate[NewUsers](_: PromoCode, _: Country, plans.default.id)
      ).flatMap(_.toOption).map(vp => vp.promotion.asTracking.map(_ => Seq(PromotionTrackingCode -> vp.code.get)).toRight(vp.code))
       .getOrElse(Right(Seq.empty))

      val resolvedSupplierCode = supplierCode orElse request.session.get(SupplierTrackingCode).map(SupplierCode) // query param wins
      val trackingCodeSessionData = promo.right.toSeq.flatten
      val supplierCodeSessionData = resolvedSupplierCode.map(code => Seq(SupplierTrackingCode -> code.get)).getOrElse(Seq.empty)

      Ok(views.html.checkout.payment(
        personalData = personalData,
        productData = productData,
        country = determinedCountry,
        countryGroup = determinedCountryGroup,
        defaultCurrency = determinedCurrency,
        countriesWithCurrency = determinedCountriesWithCurrency,
        touchpointBackendResolution = resolution,
        promoCode = promo.left.toOption,
        supplierCode = resolvedSupplierCode,
        edition = model.DigitalEdition.getForCountryGroup(determinedCountryGroup)
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
          handlePaymentGatewayError(e.paymentError, e.purchaserIds)

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
        SessionKeys.Currency -> subscribeRequest.genericData.personalData.currency.toString
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
        catalog.allSubs.flatten.map(plan => RatePlanPrice(plan.id, plan.charges)).map { ratePlanPrice =>
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

  // PaymentGatewayError should be logged at WARN level
  private def handlePaymentGatewayError(e: PaymentGatewayError, purchaserIds: PurchaserIdentifiers) = {

    def handleError(msg: String, errType: String) = {
      logger.warn(s"$purchaserIds could not subscribe: $msg")
      Forbidden(Json.obj("type" -> errType, "message" -> msg))
    }

    // Custom message to make sure no sensitive information is revealed.
    e.errType match {
      case InsufficientFunds =>
        handleError("Your card has insufficient funds", "InsufficientFunds")

      case TransactionNotAllowed =>
        handleError("Your card does not support this type of purchase", "TransactionNotAllowed")

      case RevocationOfAuthorization =>
        handleError(
          "Cardholder has requested all payments to be stopped on this card",
          "RevocationOfAuthorization")

      case _ =>
        handleError("Your card was declined", "PaymentGatewayError")
    }
  }
}
