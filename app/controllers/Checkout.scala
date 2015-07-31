package controllers

import actions.CommonActions._
import com.gu.identity.play.IdUser
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.SubscriptionData
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc
import play.api.mvc._
import services.CheckoutService.CheckoutResult
import services._
import utils.TestUsers
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import AuthenticationService.authenticatedUserFor

object Checkout extends Controller with LazyLogging {

  private val goCardlessService = GoCardlessService

  def renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>

    val authUserOpt = authenticatedUserFor(request)
    val touchpointBackend =
      TouchpointBackend.forRequest(request.cookies.get(Testing.UnauthenticatedTestUserCookieName).map(_.value))

    def fillForm(): Future[Form[SubscriptionData]] = for {
      fullUserOpt <-authUserOpt.fold[Future[Option[IdUser]]](Future.successful(None))(au => IdentityService.userLookupByScGuU(AuthCookie(au.authCookie)))
    } yield {
        fullUserOpt.map { idUser =>
          SubscriptionsForm().fill(SubscriptionData.fromIdUser(idUser))
        } getOrElse {
          SubscriptionsForm()
        }
      }

    for {
      filledForm <- fillForm()
    } yield {
      Ok(views.html.checkout.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, touchpointBackend.zuoraService.products))
    }
  }

  val parseCheckoutForm = parse.form[SubscriptionData](SubscriptionsForm.subsForm, onErrors = formWithErrors => {
    logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
    BadRequest
  })

  def handleCheckout = GoogleAuthenticatedStaffAction.async(parseCheckoutForm) { implicit request =>
    val formData = request.body
    val idUserOpt = authenticatedUserFor(request)

    val touchpointBackend = TouchpointBackend.forRequest(Some(formData.personalData.firstName))

    touchpointBackend.checkoutService.processSubscription(formData, idUserOpt).map { case CheckoutResult(_, userIdData, subscription) =>
      val passwordForm = userIdData.toGuestAccountForm
      Ok(view.thankyou(subscription.name, formData.personalData, passwordForm))
    }
  }

  def mandatePDF = GoogleAuthenticatedStaffAction.async { implicit request =>
    SubscriptionsForm.paymentDataForm.bindFromRequest.fold(
      handleWithBadRequest,
      paymentData =>
        goCardlessService.mandatePDFUrl(paymentData).map(url =>
          Ok(Json.obj("mandatePDFUrl" -> url))
        )
    )
  }

  def processFinishAccount = GoogleAuthenticatedStaffAction.async { implicit request =>
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
