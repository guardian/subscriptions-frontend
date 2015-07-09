package forms

import model.{AddressData, PaymentData, PersonalData, SubscriptionData}

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
    "payment" -> paymentDataMapping,
    "ratePlanId" -> text
  )(SubscriptionData.apply)(SubscriptionData.unapply))

  def apply() = subsForm
}
