package controllers

import actions.CommonActions._
import com.gu.i18n.{Country, CountryGroup, GBP}
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.promo.Writers._
import com.gu.stripe.Stripe
import com.gu.zuora.soap
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import configuration.Config._
import forms.{FinishAccountForm, SubscriptionsForm}
import model.{DirectDebitData, SubscriptionData, SubscriptionRequestData}
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services._
import tracking.ActivityTracking
import tracking.activities.{CheckoutReachedActivity, MemberData, SubscriptionRegistrationActivity}
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.CountryWithCurrency

import scala.Function.const
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.scalaFuture._

object Checkout extends Controller with LazyLogging with ActivityTracking with CatalogProvider {
  object SessionKeys {
    val SubsName = "newSubs_subscriptionName"
    val RatePlanId = "newSubs_ratePlanId"
    val UserId = "newSubs_userId"
    val IdentityGuestPasswordSettingToken = "newSubs_token"
    val AppliedPromoCode = "newSubs_appliedPromoCode"
  }

  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def getEmptySubscriptionsForm(promoCode: Option[PromoCode]) =
    SubscriptionsForm().bind(Map("promoCode" -> promoCode.fold("")(_.get)))

  def renderCheckout(countryGroup: CountryGroup, promoCode: Option[PromoCode]) = NoCacheAction.async { implicit request =>

    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    trackAnon(CheckoutReachedActivity(countryGroup))
    val authenticatedUser = authenticatedUserFor(request)

    val subscriptionData = (for {
      authUser <- OptionT(Future.successful(authenticatedUser))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
    } yield SubscriptionData.fromIdUser(promoCode)(idUser)).run

    val plans = catalog.planMap.values.toSeq
    val defaultPlan = catalog.digipackMonthly

    subscriptionData map { subsData =>
      val form = subsData.fold(getEmptySubscriptionsForm(promoCode)) { data => SubscriptionsForm().fill(data) }
      val countryGroupWithDefault =
        subsData.flatMap { data => CountryGroup.byCountryNameOrCode(data.personalData.address.countryName) }
                .getOrElse(countryGroup)
      val desiredCurrency = countryGroupWithDefault.currency
      val supportedCurrencies = defaultPlan.currencies
      val currency = if (supportedCurrencies.contains(desiredCurrency)) desiredCurrency else GBP

      Ok(views.html.checkout.payment(form = form,
                                     userIsSignedIn = authenticatedUser.isDefined,
                                     plans = plans,
                                     defaultPlan = defaultPlan,
                                     countryGroup = countryGroupWithDefault,
                                     currency = currency,
                                     countriesWithCurrency = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
                                     touchpointBackendResolution = resolution))
    }
  }

  val parseCheckoutForm: BodyParser[SubscriptionData] = parse.form[SubscriptionData](SubscriptionsForm.subsForm, onErrors = formWithErrors => {
    logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
    BadRequest
  })

  def handleCheckout = NoCacheAction.async(parseCheckoutForm) { implicit request =>
    val formData = request.body
    val idUserOpt = authenticatedUserFor(request)

    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(NameEnteredInForm, formData)
    implicit val tpBackend = resolution.backend

    val requestData = SubscriptionRequestData(ProxiedIP.getIP(request))
    val checkoutResult = checkoutService.processSubscription(formData, idUserOpt, requestData)

    checkoutResult.map { result =>

      val productData = Seq(
        SessionKeys.SubsName -> result.subscribeResult.subscriptionName,
        SessionKeys.RatePlanId -> formData.productRatePlanId.get
      )

      val userSessionFields = result.userIdData match {
        case GuestUser(UserId(userId), IdentityToken(token)) =>
          Seq(
            SessionKeys.UserId -> userId,
            SessionKeys.IdentityGuestPasswordSettingToken -> token
          )
        case _ => Seq()
      }

      val appliedPromoCode = result.validPromoCode.fold(Seq.empty[(String,String)])(validPromoCode => Seq(SessionKeys.AppliedPromoCode -> validPromoCode.get))

      val session = (productData ++ userSessionFields ++ appliedPromoCode).foldLeft(request.session) { _ + _ }

      catalog.find(formData.productRatePlanId).foreach { plan =>
        trackAnon(SubscriptionRegistrationActivity(MemberData(result, formData, plan.billingPeriod)))
      }

      Ok(Json.obj("redirect" -> routes.Checkout.thankYou().url)).withSession(session)
    }.recover {
      case err: soap.Error if err.code == "TRANSACTION_FAILED" => Forbidden
      case err: Stripe.Error => Forbidden
    }
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
      subsName <- session.get(SessionKeys.SubsName)
      ratePlanId <- session.get(SessionKeys.RatePlanId)
    } yield (subsName, ratePlanId)

    // TODO If some pieces of information are missing, redirect to an empty form. Is it the expected behaviour?
    def redirectToEmptyForm = Redirect(routes.Checkout.renderCheckout(CountryGroup.UK, None))

    sessionInfo.fold(redirectToEmptyForm) { case (subsName, ratePlanId) =>
      val passwordForm = authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.UserId)
          token <- session.get(SessionKeys.IdentityGuestPasswordSettingToken)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      } {
        const(None)
      } // Don't display the user registration form if the user is logged in

      val plan = catalog.unsafeFindPaid(ProductRatePlanId(ratePlanId))

      val promotion = session.get(SessionKeys.AppliedPromoCode).flatMap(code => resolution.backend.promoService.findPromotion(PromoCode(code)))

      Ok(view.thankyou(subsName, passwordForm, resolution, plan, promotion))
    }
  }

  def validatePromoCode(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country) = NoCacheAction { implicit request =>

    val tpBackend = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies).backend
    val fallabackPromoCode = demoPromo("UAT").codes.last.get

    tpBackend.promoService.findPromotion(promoCode)
      .fold(NotFound(Json.obj("errorMessage" -> s"We can't find that code, why not try: $fallabackPromoCode \uD83D\uDE09?"))){ promo =>
        val result = promo.validateFor(prpId, country)
        val body = Json.obj(
          "promotion" -> Json.toJson(promo),
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
}
