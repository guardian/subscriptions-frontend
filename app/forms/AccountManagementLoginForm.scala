package forms

import play.api.data.Form
import play.api.data.Forms._

case class AccountManagementLoginRequest(subscriptionId: String, lastname: String, postcode: String)

object AccountManagementLoginForm {

  val mappings = Form(mapping(
    "subscriptionId" -> text(minLength = 5, maxLength = 50),
    "lastname" -> text(minLength = 1, maxLength = 50),
    "postcode" -> text(minLength = 2, maxLength = 10)
  )(AccountManagementLoginRequest.apply)(AccountManagementLoginRequest.unapply))

}
