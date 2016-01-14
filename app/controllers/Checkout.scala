package controllers

import actions.CommonActions._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.stripe.Stripe
import com.gu.zuora.soap
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
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

import scala.Function.const
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.std.scalaFuture._
import scala.concurrent.Future
import scalaz.OptionT

object Checkout extends Controller with LazyLogging with ActivityTracking with CatalogProvider {


  object SessionKeys {
    val SubsName = "newSubs_subscriptionName"
    val RatePlanId = "newSubs_ratePlanId"
    val UserId = "newSubs_userId"
    val IdentityGuestPasswordSettingToken = "newSubs_token"
  }


  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def renderCheckout = NoCacheAction.async { implicit request =>

    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    trackAnon(CheckoutReachedActivity("United Kingdom"))
    val authenticatedUser = authenticatedUserFor(request)

    val authenticatedUserForm = (for {
      authUser <- OptionT(Future.successful(authenticatedUser))
      idUser <- OptionT(IdentityService.userLookupByCredentials(authUser.credentials))
    } yield SubscriptionsForm().fill(SubscriptionData.fromIdUser(idUser))).run

    val form = authenticatedUserForm.map(_.getOrElse(SubscriptionsForm()))
    val plans = catalog.planMap.values.toSeq
    val defaultPlan = catalog.digipackMonthly

    form map { f =>
      Ok(views.html.checkout.payment(form = f,
                                     userIsSignedIn = authenticatedUser.isDefined,
                                     plans = plans,
                                     defaultPlan = defaultPlan,
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
      val userSessionFields = result.userIdData match {
        case GuestUser(UserId(userId), IdentityToken(token)) =>
          Seq(
            SessionKeys.UserId -> userId,
            SessionKeys.IdentityGuestPasswordSettingToken -> token
          )
        case _ => Seq()
      }

      val session = (Seq(
        SessionKeys.SubsName -> result.subscribeResult.name,
        SessionKeys.RatePlanId -> formData.productRatePlanId.get
      ) ++ userSessionFields).foldLeft(request.session) { _ + _ }

      catalog.find(formData.productRatePlanId).foreach { plan =>
        trackAnon(SubscriptionRegistrationActivity(MemberData(result, formData, plan.billingPeriod)))
      }

      Ok(Json.obj("redirect" -> routes.Checkout.thankYou().url)).withSession(session);
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
    def redirectToEmptyForm = Redirect(routes.Checkout.renderCheckout())

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
      Ok(view.thankyou(subsName, passwordForm, resolution, plan))
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
