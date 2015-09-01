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
import scala.Function.const

object Checkout extends Controller with LazyLogging {
  object SessionKeys {
    val subsName = "newSubs_subscriptionName"
    val ratePlanId = "newSubs_ratePlanId"
    val userId = "newSubs_userId"
    val token = "newSubs_token"
  }

  def zuoraService(implicit res: TouchpointBackend.Resolution): ZuoraService =
    res.backend.zuoraService

  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def renderCheckout = NoCacheAction.async { implicit request =>
    implicit val resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

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
      products <- zuoraService.products
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

    implicit val touchpointBackendResolution = TouchpointBackend.forRequest(NameEnteredInForm, formData)
    val requestData = SubscriptionRequestData(request.remoteAddress)

    checkoutService.processSubscription(formData, idUserOpt, requestData).map { case CheckoutResult(_, userIdData, subscription) =>
      val userSessionFields = userIdData match {
        case GuestUser(UserId(userId), IdentityToken(token)) =>
          Seq(
            SessionKeys.userId -> userId,
            SessionKeys.token -> token
          )
        case _ => Seq()
      }

      val session = (Seq(
        SessionKeys.subsName -> subscription.name,
        SessionKeys.ratePlanId -> formData.ratePlanId
      ) ++ userSessionFields).foldLeft(request.session) { _ + _ }

      Redirect(routes.Checkout.thankYou()).withSession(session)
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

  def thankYou = NoCacheAction.async { implicit request =>
    implicit val touchpointBackendResolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SessionKeys.subsName)
      ratePlanId <- session.get(SessionKeys.ratePlanId)
    } yield (subsName, ratePlanId)

    // TODO If some info are missing, redirect to an empty form. Is it the expected behaviour?
    def redirectToEmptyForm = Future { Redirect(routes.Checkout.renderCheckout()) }

    sessionInfo.fold(redirectToEmptyForm) { case (subsName, ratePlanId) =>
      val passwordForm = authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.userId)
          token  <- session.get(SessionKeys.token)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      }{ const(None) } // Don't display the user registration form if the user is logged in

      zuoraService.products.map { products =>
        val product = products.find(_.ratePlanId == ratePlanId).getOrElse(
          throw new NoSuchElementException(s"Could not find a product with rate plan id $ratePlanId")
        )
        Ok(view.thankyou(subsName, passwordForm, touchpointBackendResolution, product))
      }
    }
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
