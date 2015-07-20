package controllers

import actions.CommonActions._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.SubscriptionData
import play.api.libs.json._
import play.api.mvc._
import services.CheckoutService.CheckoutResult
import services._
import views.html.{checkout => view}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging {
  def zuoraService(implicit r: RequestWithServices[_]) = r.zuoraService
  def salesforceService(implicit r: RequestWithServices[_]) = r.salesforceService
  def checkoutService(implicit r: RequestWithServices[_]) = r.checkoutService
  def identityUser(implicit r: RequestWithServices[_]) = r.identityUser

  def renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    identityUser.map { idUserOpt =>
      val form = idUserOpt.map { idUser =>
        val data = SubscriptionData.fromIdUser(idUser)
        SubscriptionsForm().fill(data)
      } getOrElse {
        SubscriptionsForm()
      }
      Ok(views.html.checkout.payment(form, userIsSignedIn = idUserOpt.isDefined, zuoraService.products))
    }
  }

  def handleCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    SubscriptionsForm().bindFromRequest.fold(
      formWithErrors => {
        identityUser.map { idUserOpt =>
          logger.error(s"Backend form validation failed. Please make sure that the front-end and the backend validations are in sync (validation errors: ${formWithErrors.errors}})")
          BadRequest(view.payment(formWithErrors, userIsSignedIn = idUserOpt.isDefined, zuoraService.products))
        }
      },

      formData => {
        val idUserOpt = AuthenticationService.authenticatedUserFor(request)
        val authCookie = request.identityCookie.map(cookie => AuthCookie(cookie.value))

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
}
