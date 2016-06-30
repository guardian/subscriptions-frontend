package controllers

import actions.CommonActions._
import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.{BillingPeriod, Digipack, Paper, ProductFamily}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.subscriptions.DigipackPlan
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.error.CheckoutService._
import model.error.SubsError
import model._
import play.api.data.{Form, FormError}
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services._
import tracking.ActivityTracking
import tracking.activities.{CheckoutReachedActivity, MemberData, SubscriptionRegistrationActivity}
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{CountryWithCurrency, DigipackSubscriptionsForm, PaperSubscriptionsForm, PlanList}

import scala.Function.const
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.{NonEmptyList, OptionT, \/}
import model.IdUserOps._
import touchpoint.TouchpointBackendConfig.BackendType
import utils.TestUsers

object Checkout extends Controller with LazyLogging with ActivityTracking with CatalogProvider {
  object SessionKeys {
    val SubsName = "newSubs_subscriptionName"
    val RatePlanId = "newSubs_ratePlanId"
    val UserId = "newSubs_userId"
    val IdentityGuestPasswordSettingToken = "newSubs_token"
    val AppliedPromoCode = "newSubs_appliedPromoCode"
    val Currency = "newSubs_currency"
  }

  import SessionKeys.{Currency => _, UserId => _, _}



  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def getEmptySubscriptionsForm(promoCode: Option[PromoCode])(implicit res: TouchpointBackend.Resolution) =
    SubscriptionsForm.subsForm.bind(Map("promoCode" -> promoCode.fold("")(_.get)))

  def renderPaperCheckout(countryGroup: CountryGroup, promoCode: Option[PromoCode]) = AuthorisedTester.async { req =>
    renderCheckout(countryGroup, promoCode, productFamily = Paper).apply(req)
  }

  def renderCheckout(countryGroup: CountryGroup, promoCode: Option[PromoCode], productFamily: ProductFamily = Digipack) = NoSubAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    trackAnon(CheckoutReachedActivity(countryGroup))
    val authenticatedUser = authenticatedUserFor(request)

    val idUser = (for {
      authUser <- OptionT(Future.successful(authenticatedUser))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
    } yield idUser).run

    val plans = catalog.planMap.values.toSeq
    val defaultPlan = catalog.digipackMonthly

    idUser map { user =>

      val productData: views.support.SubscriptionsForm = productFamily match {
        case Paper =>
          val catalog = tpBackend.catalogService.paperCatalog.get
          PaperSubscriptionsForm(user.map(_.address), PlanList(catalog.everyday, catalog.sixday, catalog.weekend))
        case _ =>
          val catalog = tpBackend.catalogService.digipackCatalog
          DigipackSubscriptionsForm(PlanList(catalog.digipackMonthly, catalog.digipackQuarterly, catalog.digipackYearly))
      }

      val personalData = user.map(PersonalData.fromIdUser)
      val countryOpt = personalData.flatMap(data => data.address.country)
      val countryGroupWithDefault = countryOpt.fold(countryGroup)(c => CountryGroup.byCountryCode(c.alpha2).getOrElse(countryGroup))
      val country = countryOpt orElse countryGroupWithDefault.defaultCountry
      val desiredCurrency = countryGroupWithDefault.currency
      val supportedCurrencies = defaultPlan.currencies
      val defaultCurrency = if (supportedCurrencies.contains(desiredCurrency)) desiredCurrency else GBP

      Ok(views.html.checkout.payment(
        personalData = personalData,
        productData = productData,
        defaultPlan = defaultPlan,
        country = country,
        countryGroup = countryGroupWithDefault,
        defaultCurrency = defaultCurrency,
        countriesWithCurrency = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
        touchpointBackendResolution = resolution,
        promoCode = promoCode))
    }
  }

  def handlePaperCheckout = AuthorisedTester.async { req =>
    handleCheckout(allowPaper = true).apply(req)
  }

  def handleCheckout(allowPaper: Boolean = false) = NoSubAjaxAction.async { implicit request =>

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

    subscribeRequest.productData.left.foreach { _ =>
      if (!allowPaper) throw new Exception("paper not allowed")
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
      val session = (productData ++ userSessionFields ++ appliedPromoCode).foldLeft(request.session.-(AppliedPromoCode)) {
        _ + _
      }

      catalog.find(subscribeRequest.productRatePlanId).foreach { plan =>
        trackAnon(SubscriptionRegistrationActivity(MemberData(r, subscribeRequest, plan.billingPeriod)))
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

  def thankYou = NoCacheAction { implicit request =>

    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SubsName)
      ratePlanId <- session.get(RatePlanId)
      currencyStr <- session.get(SessionKeys.Currency)
      currency <- Currency.fromString(currencyStr)
    } yield (subsName, ratePlanId, currency)

    def redirectToEmptyForm = Redirect(routes.Checkout.renderCheckout(CountryGroup.UK, None)).withNewSession

    sessionInfo.fold(redirectToEmptyForm) { case (subsName, ratePlanId, currency) =>
      val passwordForm = authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.UserId)
          token <- session.get(IdentityGuestPasswordSettingToken)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      } {
        const(None)
      } // Don't display the user registration form if the user is logged in


      val prpId = ProductRatePlanId(ratePlanId)

      val plan =
        (tpBackend.catalogService.digipackCatalog.findPaid(prpId) orElse
        tpBackend.catalogService.paperCatalog.flatMap(_.findCurrent(prpId))).get

      val promotion = session.get(AppliedPromoCode).flatMap(code => resolution.backend.promoService.findPromotion(PromoCode(code)))

      Ok(view.thankyou(subsName, passwordForm, resolution, plan, promotion, currency))
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
