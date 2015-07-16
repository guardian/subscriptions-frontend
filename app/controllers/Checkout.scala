package controllers

import actions.CommonActions._
import com.gu.identity.play.IdUser
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.SubscriptionData
import play.api.libs.json._
import play.api.mvc._
import services.CheckoutService.CheckoutResult
import services.{CheckoutService, _}
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging {
  private val zuoraService = TouchpointBackend.Normal.zuoraService
  lazy val checkoutService = new CheckoutService(IdentityService, SalesforceService, zuoraService)

  def identityCookieOpt(implicit request: Request[_]): Option[Cookie] =
    request.cookies.find(_.name == "SC_GU_U")

  def renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    getIdentityUserByCookie.map { idUserOpt =>
      val form = idUserOpt.map { idUser =>
        val data = SubscriptionData.fromIdUser(idUser)
        SubscriptionsForm().fill(data)
      } getOrElse {
        SubscriptionsForm()
      }
      //TODO when implementing test-users this requires updating to supply data to correct location
      val touchpointBackend = TouchpointBackend.Normal
      Ok(views.html.checkout.payment(form, userIsSignedIn = idUserOpt.isDefined, zuoraService.products))
    }
  }

  def handleCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    SubscriptionsForm().bindFromRequest.fold(
      formWithErrors => {
        getIdentityUserByCookie.map { idUserOpt =>
          logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
          //TODO when implementing test-users this requires updating to supply data to correct location
          val touchpointBackend = TouchpointBackend.Normal
          BadRequest(view.payment(formWithErrors, userIsSignedIn = idUserOpt.isDefined, zuoraService.products))
        }
      },

      formData => {
        val idUserOpt = AuthenticationService.authenticatedUserFor(request)
        val authCookie = identityCookieOpt.map(cookie => AuthCookie(cookie.value))

        checkoutService.processSubscription(formData, idUserOpt, authCookie).map { case CheckoutResult(_, userIdData, subscription) =>
          val passwordForm = userIdData.toGuestAccountForm
          Ok(view.thankyou(subscription.name, formData.personalData, passwordForm))
        }
      }
    )
  }

  def processFinishAccount = GoogleAuthenticatedStaffAction.async { implicit request =>
    FinishAccountForm().bindFromRequest.fold({ formWithErrors =>
      Future {
        BadRequest(Json.obj(
          "error" -> "Invalid form submissions",
          "invalidFields" -> formWithErrors.errors.map(_.key)
        ))
      }
    }, guestAccountData => {
      IdentityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token))
        .map { _ =>
          Ok(Json.obj("profileUrl" -> webAppProfileUrl.toString()))
        }
    })
  }

  def checkIdentity(email: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  private def getIdentityUserByCookie(implicit request: Request[_]): Future[Option[IdUser]] =
    identityCookieOpt.fold(Future.successful(None: Option[IdUser])) { cookie =>
      IdentityService.userLookupByScGuU(AuthCookie(cookie.value))
    }
}
