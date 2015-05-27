package controllers

import actions.CommonActions._
import model.{AddressData, PaymentData, PersonalData, SubscriptionData}
import play.api.mvc._
import services.CheckoutService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller {

  import play.api.data.Forms._
  import play.api.data._

  val subscriptionForm = Form(mapping(
    "" -> mapping(
      "first" -> text,
      "last" -> text,

      "" -> tuple(
        "email" -> email,
        "confirm" -> email)
        .verifying("Emails don't match", email => email._1 == email._2)
        .transform[String](
          email => email._1, // Transform to a single field
          email => (email, email) // Reverse transform from a single field to multiple
        ),

      "" -> mapping(
        "house" -> text,
        "street" -> text,
        "town" -> text,
        "postcode" -> text
      )(AddressData.apply)(AddressData.unapply)

    )(PersonalData.apply)(PersonalData.unapply),

    "" -> mapping(
      "account" -> text,
      "sortcode1" -> number(10, 99),
      "sortcode2" -> number(10, 99),
      "sortcode3" -> number(10, 99),
      "holder" -> text
    )(PaymentData.apply)(PaymentData.unapply)

  )(SubscriptionData.apply)(SubscriptionData.unapply))

  val renderCheckout = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.payment(subscriptionForm)))

  val handleCheckout = NoCacheAction.async { implicit request =>
    val formWithData = subscriptionForm.bindFromRequest

    if (formWithData.hasErrors) {
      println(s">>>${formWithData.errors}")
      Future.successful(Ok(views.html.checkout.payment(formWithData)))
    }
    else {
      val subscriptionData: SubscriptionData = formWithData.get
      println(s">>>$subscriptionData")
      CheckoutService.processSubscription(subscriptionData, request.cookies.find(_.name == "SC_GU_U").map(_.value))
        .map(_ => Redirect(routes.Checkout.thankyou()))
    }
  }

  def thankyou = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.thankyou()))
}