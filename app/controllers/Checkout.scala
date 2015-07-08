package controllers

import actions.CommonActions._
import com.gu.identity.play.{IdUser, PrivateFields}
import com.typesafe.scalalogging.LazyLogging
import forms.{FinishAccountForm, SubscriptionsForm}
import model.PersonalData
import play.api.libs.json._
import play.api.mvc._
import services.CheckoutService
import services.CheckoutService.CheckoutResult
import services._

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

      Ok(views.html.checkout.payment(form, userIsSignedIn = idUserOpt.isDefined))
    }
  }

  def handleCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    SubscriptionsForm().bindFromRequest.fold(
      formWithErrors => {
        for (idUserOpt <- getIdentityUserByCookie(request))
          yield BadRequest(views.html.checkout.payment(formWithErrors, userIsSignedIn = idUserOpt.isDefined))
      },
      subscriptionData => {
        val idUserOpt = AuthenticationService.authenticatedUserFor(request)
        CheckoutService.processSubscription(subscriptionData, idUserOpt).map {
          case CheckoutResult(_, guestUser: GuestUser, _) =>
            Some(FinishAccountForm().bind(guestUser.toFormParams))
          case CheckoutResult(_, registeredUser:MinimalIdUser, _) =>
            updateStoredRegisteredUserDetails(subscriptionData.personalData, registeredUser.id, request.cookies.find(_.name == "SC_GU_U"))
            None
        }.map(formOpt => Ok(views.html.checkout.thankyou(formOpt)))
      }
    )
  }

  def thankyou = GoogleAuthenticatedStaffAction.async { implicit request =>
    Future {
      val form = FinishAccountForm().bindFromRequest()
      Ok(views.html.checkout.thankyou(Some(form)))
    }
  }

  def processFinishAccount = GoogleAuthenticatedStaffAction.async { implicit request =>
    FinishAccountForm().bindFromRequest.fold( formWithErrors => {
      Future {
        BadRequest(views.html.checkout.thankyou(Some(formWithErrors)))
      }
    }, guestAccountData => {
      IdentityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token))
        .map(_ => Ok(views.html.checkout.alldone()))
    })
  }

  def checkIdentity(email: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  def updateStoredRegisteredUserDetails(personalData: PersonalData, userId: UserId, authCookie: Option[Cookie]): Unit =
    authCookie.foreach(cookie => IdentityService.updateUserDetails(personalData, userId, AuthCookie(cookie.value)))

  private def getIdentityUserByCookie(request: Request[_]): Future[Option[IdUser]] =
    request.cookies.find(_.name == "SC_GU_U").fold(Future.successful(None: Option[IdUser])) { cookie =>
      IdentityService.userLookupByScGuU(cookie.value)
    }
}
