package controllers

import actions.CommonActions._
import com.gu.identity.play.IdUser
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.{PaymentData, SubscriptionData, SubscriptionRequestData}
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services.CheckoutService.CheckoutResult
import services.TouchpointBackend.Resolution
import services._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging {

  def renderCheckout = NoCacheAction.async { implicit request =>

    val authUserOpt = authenticatedUserFor(request)

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
      resolution: Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
      products <- resolution.backend.zuoraService.products
    } yield {
      Ok(views.html.checkout.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, products, resolution))
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
    val requestData = SubscriptionRequestData(request.remoteAddress)

    for {
      CheckoutResult(_, userIdData, subscription) <- touchpointBackendResolution.backend.checkoutService.processSubscription(formData, idUserOpt, requestData)
      passwordForm = userIdData.toGuestAccountForm
      products <- touchpointBackendResolution.backend.zuoraService.products
      subscriptionProduct = products.find(_.ratePlanId == formData.ratePlanId).getOrElse {
        throw new NoSuchElementException(s"Could not find a product with rate plan id: ${formData.ratePlanId}")
      }
    } yield Ok(view.thankyou(subscription.name, formData.personalData, passwordForm, touchpointBackendResolution, subscriptionProduct))
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

  val parsePaymentForm: BodyParser[PaymentData] = parse.form[PaymentData](Form(SubscriptionsForm.paymentDataMapping))

  def checkAccount = CachedAction.async(parsePaymentForm) { implicit request =>
    for {
      isAccountValid <- GoCardlessService.checkBankDetails(request.body)
    } yield Ok(Json.obj("accountValid" -> isAccountValid))
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
