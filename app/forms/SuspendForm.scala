package forms

import org.joda.time.{Days, LocalDate}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.JodaForms._
import play.api.libs.Crypto
import scalaz.syntax.std.boolean._

case class Suspension(startDate: LocalDate, endDate: LocalDate) {

  def asDays(): Days = Days.daysBetween(startDate, endDate)

}

object SuspendForm {

  val mappings = Form(mapping(
    "startDate" -> jodaLocalDate("d MMMM y"),
    "endDate" -> jodaLocalDate("d MMMM y")
  )(Suspension.apply)(Suspension.unapply))

}
