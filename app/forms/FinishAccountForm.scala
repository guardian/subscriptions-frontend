package forms

import model.GuestAccountData
import services.{IdentityToken, UserId}

object FinishAccountForm {
  import play.api.data.Forms._
  import play.api.data._

  def apply(): Form[GuestAccountData] = Form(mapping(
    "password" -> text(minLength = 6),
    UserId.paramName -> text,
    IdentityToken.paramName -> text
  )(GuestAccountData.apply)(GuestAccountData.unapply))
}
