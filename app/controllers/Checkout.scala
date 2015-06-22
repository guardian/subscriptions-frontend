package controllers

import actions.CommonActions._
import com.gu.identity.play.PrivateFields
import model.{AddressData, PaymentData, PersonalData, SubscriptionData}
import play.api.data.Form
import play.api.mvc._
import services.{AuthenticationService, CheckoutService, IdentityService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Checkout extends Controller {
  
  val renderCheckout = GoogleAuthenticatedStaffAction.async { implicit request =>
    val idUserFutureOpt = request.cookies.find(_.name == "SC_GU_U").map {cookie =>
      IdentityService.userLookupByScGuU(cookie.value)
    }.getOrElse(Future.successful(None))

    for (idUserOpt <- idUserFutureOpt) yield {
      def idUserData(keyName: String, fieldName: PrivateFields => Option[String]): Option[(String, String)] =
        for {
          idUser <- idUserOpt
          fields <- idUser.privateFields
          field <- fieldName(fields)
        } yield keyName -> field


      val form = SubscriptionsForm().copy(
        data =  (
          idUserData("personal.first", _.firstName) ++
          idUserData("personal.last", _.secondName) ++
          idUserOpt.map("personal.emailValidation.email" -> _.primaryEmailAddress) ++
          idUserOpt.map("personal.emailValidation.confirm" -> _.primaryEmailAddress) ++
          idUserData("personal.address.address1", _.address1) ++
          idUserData("personal.address.address2", _.address2) ++
          idUserData("personal.address.town", _.address3) ++
          idUserData("personal.address.postcode", _.postcode)
          ).toMap
      )

      Ok(views.html.checkout.payment(form))
    }
  }

  val handleCheckout = GoogleAuthenticatedStaffAction.async(parse.form(
    form = SubscriptionsForm(),
    onErrors = (formWithErrors: Form[model.SubscriptionData]) => BadRequest(views.html.checkout.payment(formWithErrors))
  )) { implicit request =>

    CheckoutService.processSubscription(request.body, AuthenticationService.authenticatedUserFor(request))
      .map(_ => Redirect(routes.Checkout.thankyou()))
  }

  def thankyou = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.thankyou()))
}

object SubscriptionsForm {
  import play.api.data.Forms._
  import play.api.data._

  private val addressDataMapping = mapping(
    "address1" -> text,
    "address2" -> text,
    "town" -> text,
    "postcode" -> text
  )(AddressData.apply)(AddressData.unapply)

  private val emailMapping = tuple(
    "email" -> email,
    "confirm" -> email)
    .verifying("Emails don't match", email => email._1 == email._2)
    .transform[String](
      email => email._1, // Transform to a single field
      email => (email, email) // Reverse transform from a single field to multiple
    )

  private val personalDataMapping = mapping(
    "first" -> text,
    "last" -> text,
    "emailValidation" -> emailMapping,
    "address" -> addressDataMapping
  )(PersonalData.apply)(PersonalData.unapply)

  private val paymentDataMapping = mapping(
    "account" -> text(8, 8),
    "sortcode1" -> text(2, 2),
    "sortcode2" -> text(2, 2),
    "sortcode3" -> text(2, 2),
    "holder" -> text
  )(PaymentData.apply)(PaymentData.unapply)

  private val subsForm = Form(mapping(
    "personal" -> personalDataMapping,
    "payment" -> paymentDataMapping
  )(SubscriptionData.apply)(SubscriptionData.unapply))

  def apply() = subsForm
}