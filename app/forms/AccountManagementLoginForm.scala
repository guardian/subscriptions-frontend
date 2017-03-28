package forms

import play.api.data.Form
import play.api.data.Forms._

case class AccountManagementLoginRequest(subscriptionId: String, lastname: String, promoCode: Option[String])

object AccountManagementLoginForm {

  val mappings = Form(mapping(
    "subscriptionId" -> text(minLength = 1, maxLength = 50),
    "lastname" -> text(minLength = 1, maxLength = 50),
    "promoCode" -> optional(text(minLength = 1, maxLength = 50))
  )(AccountManagementLoginRequest.apply)(AccountManagementLoginRequest.unapply))

}
