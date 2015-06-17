package controllers

import actions.CommonActions._
import com.gu.identity.play.PrivateFields
import model.{AddressData, PaymentData, PersonalData, SubscriptionData}
import play.api.mvc._
import services.{IdentityService, CheckoutService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller {

  import play.api.data.Forms._
  import play.api.data._

  private val addressDataMapping = "" -> mapping(
    "house" -> text,
    "street" -> text,
    "town" -> text,
    "postcode" -> text
  )(AddressData.apply)(AddressData.unapply)

  private val emailMapping = "" -> tuple(
    "email" -> email,
    "confirm" -> email)
    .verifying("Emails don't match", email => email._1 == email._2)
    .transform[String](
      email => email._1, // Transform to a single field
      email => (email, email) // Reverse transform from a single field to multiple
    )

  private val personalDataMapping = "" -> mapping(
    "first" -> text,
    "last" -> text,
    emailMapping,
    addressDataMapping
  )(PersonalData.apply)(PersonalData.unapply)

  private val paymentDataMapping = "" -> mapping(
    "account" -> text(8, 8),
    "sortcode1" -> text(2, 2),
    "sortcode2" -> text(2, 2),
    "sortcode3" -> text(2, 2),
    "holder" -> text
  )(PaymentData.apply)(PaymentData.unapply)

  val subscriptionForm = Form(mapping(
    personalDataMapping,
    paymentDataMapping
  )(SubscriptionData.apply)(SubscriptionData.unapply))

  val renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    val idUserFutureOpt = request.cookies.find(_.name == "SC_GU_U").map {cookie =>
      IdentityService.userLookupByScGuU(cookie.value)
    }.getOrElse(Future.successful(None))

    idUserFutureOpt.map { idUserOpt =>
      val idUserData: (PrivateFields => Option[String]) => Option[String] =
        fieldName => idUserOpt.flatMap(_.privateFields.flatMap(fieldName))
      Ok(
        views.html.checkout.payment(
          idUserData(_.firstName),
          idUserData(_.secondName),
          idUserOpt.map(_.primaryEmailAddress),
          idUserData(_.billingAddress1),
          idUserData(_.billingAddress2),
          idUserData(_.billingAddress3),
          idUserData(_.billingPostcode))
      )
    }
  }

  //todo GoogleAuth the Action
  //todo handle error https://www.playframework.com/documentation/2.4.x/ScalaForms
  val handleCheckout = NoCacheAction.async(parse.form(subscriptionForm)) { implicit request =>
    CheckoutService.processSubscription(request.body, request)
      .map(_ => Redirect(routes.Checkout.thankyou()))
  }

  def thankyou = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.thankyou()))
}