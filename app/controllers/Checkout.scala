package controllers

import actions.CommonActions._
import com.gu.identity.play.IdUser
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.SubscriptionData
import model.zuora.BillingFrequency
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc
import play.api.mvc._
import services.CheckoutService.CheckoutResult
import services._
import utils.TestUsers
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import AuthenticationService.authenticatedUserFor
import utils.Prices._

object Checkout extends Controller with LazyLogging {

  def renderCheckout = NoCacheAction.async { implicit request =>

    val authUserOpt = authenticatedUserFor(request)
    val touchpointBackendResolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

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
      Ok(views.html.checkout.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, touchpointBackendResolution))
    }
  }

  val parseCheckoutForm: BodyParser[SubscriptionData] = parse.form[SubscriptionData](SubscriptionsForm.subsForm, onErrors = formWithErrors => {
    logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
    BadRequest
  })

  def handleCheckout = NoCacheAction.async(parseCheckoutForm) { implicit request =>
    val formData = request.body
    val idUserOpt = authenticatedUserFor(request)

    val touchpointBackendResolution = TouchpointBackend.forRequest(NameEnteredInForm, formData)

    touchpointBackendResolution.backend.checkoutService.processSubscription(formData, idUserOpt).map { case CheckoutResult(_, userIdData, subscription) =>
      val passwordForm = userIdData.toGuestAccountForm

      val subscriptionProduct = touchpointBackendResolution.backend.zuoraService.products.filter(_.ratePlanId == formData.ratePlanId).head

      Ok(view.thankyou(subscription.name, formData.personalData, passwordForm, touchpointBackendResolution, subscriptionProduct))
    }
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
