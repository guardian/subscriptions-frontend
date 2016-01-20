package forms

import model.CASLookup
import play.api.data.Forms._
import play.api.data._

object CASForm {
  def apply(): Form[CASLookup] = Form(
    "cas" -> mapping(
      "number" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "postcode" -> optional(text)
    )(CASLookup.apply)(CASLookup.unapply)
  )
}
