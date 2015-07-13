package controllers

import actions.CommonActions._
import com.gu.identity.play.{IdUser, PrivateFields}
import com.typesafe.scalalogging.LazyLogging
import forms.{FinishAccountForm, SubscriptionsForm}
import model.PersonalData
import play.api.libs.json._
import play.api.mvc._
import services.CheckoutService.CheckoutResult
import services.{CheckoutService, _}
import services.{CheckoutService, _}
import views.html.{checkout => view}
import configuration.Config.Identity.webAppProfileUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller with LazyLogging {

  def renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    for (idUserOpt <- getIdentityUserByCookie(request)) yield {
      def idUserData(keyName: String, fieldName: PrivateFields => Option[String]): Option[(String, String)] =
        for {
          idUser <- idUserOpt
          fields <- idUser.privateFields
          field <- fieldName(fields)
        } yield keyName -> field

      val form = SubscriptionsForm().copy(
        data = (
          idUserData("personal.first", _.firstName) ++
          idUserData("personal.last", _.secondName) ++
          idUserOpt.map("personal.emailValidation.email" -> _.primaryEmailAddress) ++
          idUserOpt.map("personal.emailValidation.confirm" -> _.primaryEmailAddress) ++
          idUserData("personal.address.address1", _.billingAddress1) ++
          idUserData("personal.address.address2", _.billingAddress2) ++
          idUserData("personal.address.town", _.billingAddress3) ++
          idUserData("personal.address.postcode", _.billingPostcode)
        ).toMap
      )

      //TODO when implementing test-users this requires updating to supply data to correct location
      val touchpointBackend = TouchpointBackend.Normal

      Ok(views.html.checkout.payment(form, userIsSignedIn = idUserOpt.isDefined, touchpointBackend.zuoraService.products))
    }
  }

  def handleCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    SubscriptionsForm().bindFromRequest.fold(
      formWithErrors => {
        for (idUserOpt <- getIdentityUserByCookie(request))
          yield {
            //TODO when implementing test-users this requires updating to supply data to correct location
            val touchpointBackend = TouchpointBackend.Normal

            BadRequest(views.html.checkout.payment(formWithErrors, userIsSignedIn = idUserOpt.isDefined, touchpointBackend.zuoraService.products))
          }
      },

      formData => {
        val idUserOpt = AuthenticationService.authenticatedUserFor(request)
        val authCookie = request.cookies.find(_.name == "SC_GU_U").map(cookie => AuthCookie(cookie.value))

        CheckoutService.processSubscription(formData, idUserOpt, authCookie).map { case CheckoutResult(_, userIdData, subscription) =>
          val passwordForm = userIdData.toGuestAccountForm
          Ok(view.thankyou(subscription.id, formData.personalData, passwordForm))
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

  private def getIdentityUserByCookie(request: Request[_]): Future[Option[IdUser]] =
    request.cookies.find(_.name == "SC_GU_U").fold(Future.successful(None: Option[IdUser])) { cookie =>
      IdentityService.userLookupByScGuU(AuthCookie(cookie.value))
    }
}
