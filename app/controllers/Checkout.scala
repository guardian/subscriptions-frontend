package controllers

import actions.CommonActions._
import com.gu.identity.play.{AuthenticatedIdUser, IdUser}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.SubscriptionData
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services.CheckoutService.CheckoutResult
import services._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging {

  def fillForm(form: Form[SubscriptionData], authUserOpt: Option[AuthenticatedIdUser]): Future[Form[SubscriptionData]] = {
    for {
      fullUserOpt <- authUserOpt.fold[Future[Option[IdUser]]](Future.successful(None))(au => IdentityService.userLookupByScGuU(AuthCookie(au.authCookie)))
    } yield fullUserOpt.map { idUser =>
      form.fill(SubscriptionData.fromIdUser(idUser))
    } getOrElse form
  }

  def renderCheckout = NoCacheAction.async { implicit request =>

    val authUserOpt = authenticatedUserFor(request)
    val touchpointBackendResolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

    for {
      filledForm <- fillForm(SubscriptionsForm(), authUserOpt)
    } yield {
      Ok(view.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, touchpointBackendResolution))
    }
  }

  val parseCheckoutForm: BodyParser[SubscriptionData] = parse.form[SubscriptionData](SubscriptionsForm.subsForm, onErrors = formWithErrors => {
    logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
    BadRequest
  })

  def handleCheckout = NoCacheAction.async { implicit request =>
    SubscriptionsForm().bindFromRequest.fold(
      formWithErrors => {
        val authUserOpt = authenticatedUserFor(request)
        val touchpointBackendResolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
        for {
          filledForm <- fillForm(formWithErrors, authUserOpt)
        } yield {
          Ok(view.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, touchpointBackendResolution))
        }
      },
      userData => {
        val touchpointBackendResolution = TouchpointBackend.forRequest(NameEnteredInForm, userData)
        touchpointBackendResolution.backend.checkoutService.processSubscription(userData, authenticatedUserFor(request)).map { case CheckoutResult(_, userIdData, subscription) =>
          val passwordForm = userIdData.toGuestAccountForm
          val subscriptionProduct = touchpointBackendResolution.backend.zuoraService.products.filter(_.ratePlanId == formData.ratePlanId).head
          Ok(view.thankyou(subscription.name, userData.personalData, passwordForm, touchpointBackendResolution, subscriptionProduct))
        }
      }
    )
  }

  def processFinishAccount = NoCacheAction.async { implicit request =>
    FinishAccountForm().bindFromRequest.fold(
      handleWithBadRequest,
      guestAccountData => {
        IdentityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token))
          .map { _ =>
          Ok(Json.obj("profileUrl" -> webAppProfileUrl.toString()))
        }
      })
  }

  def checkIdentity(email: String) = CachedAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  private def handleWithBadRequest[A](formWithErrors: Form[A]): Future[Result] =
    Future {
      logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
      BadRequest(Json.obj(
        "error" -> "Invalid form submissions",
        "invalidFields" -> formWithErrors.errors.map(_.key)
      ))
    }
}
