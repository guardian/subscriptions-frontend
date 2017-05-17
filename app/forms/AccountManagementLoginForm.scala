package forms

import play.api.data.Form
import play.api.data.Forms._

case class AccountManagementLoginRequest(unsanitisedSubscriptionId: String, lastname: String, promoCode: Option[String]) {
  val subscriptionId = unsanitisedSubscriptionId.replaceFirst("^0+", "");
}

object AccountManagementLoginForm {

  val mappings = Form(mapping(
    "subscriptionId" -> text(minLength = 1, maxLength = 50),
    "lastname" -> text(minLength = 1, maxLength = 50),
    "promoCode" -> optional(text(minLength = 1, maxLength = 50))
  )(AccountManagementLoginRequest.apply)(AccountManagementLoginRequest.unapply))

}
