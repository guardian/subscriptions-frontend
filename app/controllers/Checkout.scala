package controllers

import actions.CommonActions._
import com.gu.i18n._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.{BillingPeriod, Delivery}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.PromoCode
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
import tracking.ActivityTracking
import tracking.activities.{CheckoutReachedActivity, MemberData, SubscriptionRegistrationActivity}
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{BillingPeriod => _, _}

import scala.Function.const
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.{NonEmptyList, OptionT}

object Checkout extends Controller with LazyLogging with ActivityTracking with CatalogProvider {

  object SessionKeys {
    val SubsName = "newSubs_subscriptionName"
    val RatePlanId = "newSubs_ratePlanId"
    val UserId = "newSubs_userId"
    val IdentityGuestPasswordSettingToken = "newSubs_token"
    val AppliedPromoCode = "newSubs_appliedPromoCode"
    val Currency = "newSubs_currency"
    val StartDate = "newSubs_startDate"
  }

  import SessionKeys.{Currency => _, UserId => _, _}

  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def renderCheckout(countryGroup: CountryGroup, promoCode: Option[PromoCode], forThisPlan: String) = NoSubAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    trackAnon(CheckoutReachedActivity(countryGroup))

    val idUser = (for {
      authUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
    } yield idUser).run

    idUser map { user =>
      val planListEither = catalogue.forSlug(forThisPlan)
        .getOrElse(Left(catalogue.digipack.cheapest -> catalogue.digipack))
        .fold({ case (plan, group) => Left(PlanList(plan, group.betterThan(plan).productPlans.sortBy(_.priceGBP.amount):_*)) },
              { case (plan, group) => Right(PlanList(plan, group.betterThan(plan).productPlans.sortBy(_.priceGBP.amount):_*)) })

      val personalData = user.map(PersonalData.fromIdUser)
      val productData = ProductPopulationData(user.map(_.address), planListEither)
      val preselectedCountry = personalData.flatMap(data => data.address.country)
      val countryGroupForPreselectedCountry = preselectedCountry.flatMap(c => CountryGroup.byCountryCode(c.alpha2))
      val determinedCountryGroup = if (planListEither.isRight) {
        CountryGroup(Country.UK.name, "ukm", Some(Country.UK), List(Country.UK), CountryGroup.UK.currency, PostCode)
      } else {
        countryGroupForPreselectedCountry.getOrElse(countryGroup)
      }
      val determinedCountry = preselectedCountry orElse determinedCountryGroup.defaultCountry

      val supportedCurrencies = planListEither.fold(identity, identity).default.currencies
      val determinedCurrency = if (supportedCurrencies.contains(determinedCountryGroup.currency)) determinedCountryGroup.currency else GBP
      val determinedCountriesWithCurrency = if (planListEither.isRight) {
        determinedCountryGroup.countries.map(c => CountryWithCurrency(c, determinedCurrency))
      } else {
        CountryWithCurrency.whitelisted(supportedCurrencies, GBP)
      }

      Ok(views.html.checkout.payment(
        personalData = personalData,
        productData = productData,
        country = determinedCountry,
        countryGroup = determinedCountryGroup,
        defaultCurrency = determinedCurrency,
        countriesWithCurrency = determinedCountriesWithCurrency,
        touchpointBackendResolution = resolution,
        promoCode = promoCode))
    }
  }

  def handleCheckout = NoSubAjaxAction.async { implicit request =>

    //there's an annoying circular dependency going on here
    val tempData = SubscriptionsForm.subsForm.bindFromRequest().value
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(NameEnteredInForm, tempData)
    implicit val tpBackend = resolution.backend
    val idUserOpt = authenticatedUserFor(request)

    val srEither = tpBackend.subsForm.bindFromRequest

    val subscribeRequest = srEither.valueOr {
      e => throw new Exception(s"Backend validation failed: identityId=${idUserOpt.map(_.user.id).mkString};" +
        s" JavaScriptEnabled=${request.headers.toMap.contains("X-Requested-With")};" +
        s" ${e.map(err => s"${err.key} ${err.message}").mkString(", ")}")
    }

    val requestData = SubscriptionRequestData(ProxiedIP.getIP(request))
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

      val appliedPromoCode = r.validPromoCode.fold(Seq.empty[(String, String)])(validPromoCode => Seq(AppliedPromoCode -> validPromoCode.get))

      val subscriptionDetails = Some(StartDate -> subscribeRequest.productData.fold(_.startDate, _ => LocalDate.now).toString("d MMMM YYYY"))

      val session = (productData ++ userSessionFields ++ appliedPromoCode ++ subscriptionDetails).foldLeft(request.session.-(AppliedPromoCode)) {
        _ + _
      }

      trackAnon(SubscriptionRegistrationActivity(MemberData(r, subscribeRequest)))
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

    import tpBackend.catalogService._

    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SubsName)
      plan <- session.get(RatePlanId).map(ProductRatePlanId).flatMap(p => paperCatalog.find(p))
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
      case class RatePlanPrice(ratePlanId: ProductRatePlanId, plan: DigipackPlan[BillingPeriod])
      promo.asDiscount.map { discountPromo =>
        catalog.planMap.map { case (key, value) => RatePlanPrice(key, value) }.map { ratePlanPrice =>
          val currency = CountryGroup.byCountryCode(country.alpha2).getOrElse(CountryGroup.UK).currency
          ratePlanPrice.ratePlanId.get -> ratePlanPrice.plan.prettyPricingForDiscountedPeriod(discountPromo, currency)
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

    // TODO: Does Zuora provide a guarantee the message is safe to display to users directly?
    // For now providing custom message to make sure no sensitive information is revealed.

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
