package controllers

import actions.CommonActions._
import com.gu.identity.play.IdUser
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.soap
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.zuora.BillingFrequency
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
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging with ActivityTracking {

  object SessionKeys {
    val SubsName = "newSubs_subscriptionName"
    val RatePlanId = "newSubs_ratePlanId"
    val UserId = "newSubs_userId"
    val IdentityGuestPasswordSettingToken = "newSubs_token"
  }

  def zuoraService(implicit res: TouchpointBackend.Resolution): ZuoraService =
    res.backend.zuoraService

  def checkoutService(implicit res: TouchpointBackend.Resolution): CheckoutService =
    res.backend.checkoutService

  def renderCheckout = NoCacheAction.async { implicit request =>
    implicit val touchpointBackend = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    trackAnon(CheckoutReachedActivity("United Kingdom"))

    val authUserOpt = authenticatedUserFor(request)

    def fillForm(): Future[Form[SubscriptionData]] = for {
      fullUserOpt <- authUserOpt.fold[Future[Option[IdUser]]](Future.successful(None))(au => IdentityService.userLookupByCredentials(au.credentials))
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
      selectedProduct = products.find(p => p.frequency == BillingFrequency.Month).get
    } yield {
      Ok(views.html.checkout.payment(filledForm, userIsSignedIn = authUserOpt.isDefined, products, selectedProduct, touchpointBackend))
    }
  }

  val parseCheckoutForm: BodyParser[SubscriptionData] = parse.form[SubscriptionData](SubscriptionsForm.subsForm, onErrors = formWithErrors => {
    logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
    BadRequest
  })

  def handleCheckout = NoCacheAction.async(parseCheckoutForm) { implicit request =>
    val formData = request.body
    val idUserOpt = authenticatedUserFor(request)

    implicit val touchpointBackend = TouchpointBackend.forRequest(NameEnteredInForm, formData)
    val requestData = SubscriptionRequestData(request.remoteAddress)

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
        SessionKeys.RatePlanId -> formData.ratePlanId
      ) ++ userSessionFields).foldLeft(request.session) { _ + _ }

      for {
        products <- zuoraService.products
        product = products.find(p => p.ratePlanId == formData.ratePlanId)
      } yield {
	      product.foreach(prod => trackAnon(SubscriptionRegistrationActivity(MemberData(result, formData, prod))))
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

  def thankYou = NoCacheAction.async { implicit request =>
    implicit val touchpointBackend = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SessionKeys.SubsName)
      ratePlanId <- session.get(SessionKeys.RatePlanId)
    } yield (subsName, ratePlanId)

    // TODO If some pieces of information are missing, redirect to an empty form. Is it the expected behaviour?
    def redirectToEmptyForm = Future {
      Redirect(routes.Checkout.renderCheckout())
    }

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

      zuoraService.products.map { products =>
        val product = products.find(_.ratePlanId == ratePlanId).getOrElse(
          throw new NoSuchElementException(s"Could not find a product with rate plan id $ratePlanId")
        )
        Ok(view.thankyou(subsName, passwordForm, touchpointBackend, product))
      }
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
