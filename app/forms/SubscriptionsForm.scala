package forms

import model.{AddressData, PaymentData, PersonalData, SubscriptionData}
import play.api.data.format.Formatter
import play.api.data.format.Formats._

object SubscriptionsForm {
  import play.api.data.Forms._
  import play.api.data._

  /**
   *  Define a more convenient checkbox mapping than the default one bundled into Play.
   *
   *  When the input is present and a value is supplied it will map to true, will map to false otherwise.
   */
  private val booleanCheckboxFormatter = new Formatter[Boolean] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
      Right(data.get(key).isDefined)

    override def unbind(key: String, value: Boolean) =
      if (value) Map(key -> "on") else Map.empty
  }
  private val booleanCheckbox: Mapping[Boolean] = of[Boolean] as booleanCheckboxFormatter

  private val nameMaxLength = 50
  private val addressMaxLength = 255
  private val emailMaxLength = 240

  val addressDataMapping = mapping(
    "address1" -> text(0, addressMaxLength),
    "address2" -> text(0, addressMaxLength),
    "town" -> text(0, addressMaxLength),
    "postcode" -> text(0, addressMaxLength)
  )(AddressData.apply)(AddressData.unapply)

  val emailMapping = tuple(
    "email" -> email.verifying("This email is too long", _.length < emailMaxLength + 1),
    "confirm" -> email)
    .verifying("Emails don't match", email => email._1 == email._2)
    .transform[String](
      email => email._1, // Transform to a single field
      email => (email, email) // Reverse transform from a single field to multiple
    )

  val personalDataMapping = mapping(
    "first" -> text(0, nameMaxLength),
    "last" -> text(0, nameMaxLength),
    "emailValidation" -> emailMapping,
    "receiveGnmMarketing" -> booleanCheckbox,
    "address" -> addressDataMapping
  )(PersonalData.apply)(PersonalData.unapply)

  val paymentDataMapping = mapping(
    "account" -> text(6, 10),
    "sortcode1" -> text(2, 2),
    "sortcode2" -> text(2, 2),
    "sortcode3" -> text(2, 2),
    "holder" -> text
  )(PaymentData.apply)(PaymentData.unapply)

  val subsForm = Form(mapping(
    "personal" -> personalDataMapping,
    "payment" -> paymentDataMapping,
    "ratePlanId" -> text
  )(SubscriptionData.apply)(SubscriptionData.unapply))

  def paymentDataForm = Form("payment" -> paymentDataMapping)

  def apply() = subsForm
}
