package forms

import org.joda.time.{Days, LocalDate}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Crypto
import scalaz.syntax.std.boolean._

case class Suspension(startDate: LocalDate, endDate: LocalDate, subscriptionId: String, signedSubscriptionId: String) {

  def verifiedSubscriptionId: Option[String] =
    Suspension.isValidSubscriptionId(subscriptionId, signedSubscriptionId).option(subscriptionId)

  def asDays(): Integer = Days.daysBetween(startDate, endDate).getDays

}

object Suspension {

  def signedSubscriptionId(subscriptionId: String): String = Crypto.sign(subscriptionId)

  def isValidSubscriptionId(subscriptionId: String, signedSubscriptionId: String): Boolean = Crypto.sign(subscriptionId) == signedSubscriptionId

}

object SuspendForm {

  val mappings = Form(mapping(
    "startDate" -> jodaLocalDate("d MMMM y"),
    "endDate" -> jodaLocalDate("d MMMM y"),
    "subscriptionId" -> text,
    "signedSubscriptionId" -> text
  )(Suspension.apply)(Suspension.unapply))

}
