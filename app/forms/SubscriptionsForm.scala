package forms

import com.gu.i18n.{Title, CountryGroup, Country}
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.Address
import com.gu.memsub.Subscription.ProductRatePlanId
import model._
import play.api.data.format.Formats._
import play.api.data.format.Formatter

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

  // Unfortunately, the UI wants a country code, but Identity has a country name by default (e.g. from their MMA page),
  // So we need to convert back and forth between them
  private val countryName: Mapping[String] =
    text.verifying { code => CountryGroup.countryByCode(code).isDefined }
      .transform(
        { code => CountryGroup.countryByCode(code).fold("")(_.name)},
        { name => CountryGroup.countryByNameOrCode(name).fold("")(_.alpha2)}
      )


  private val titleFormatter = new Formatter[Option[Title]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError],Option[Title]] =
      Right(data.get(key).flatMap(Title.fromString))

    override def unbind(key: String, value: Option[Title]) =
      Map(key->value.fold("")(_.title))
  }


  val addressDataMapping = mapping(
    "address1" -> text(0, addressMaxLength),
    "address2" -> text(0, addressMaxLength),
    "town" -> text(0, addressMaxLength),
    "subdivision" -> text,
    "postcode" -> text(0, addressMaxLength),
    "country" -> countryName
  )(Address.apply)(Address.unapply)
    .verifying("address validation failed", AddressValidation.validateForCountry _)

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
    "address" -> addressDataMapping,
    "telephoneNumber" -> optional(text),
    "title"-> of(titleFormatter)
  )(PersonalData.apply)(PersonalData.unapply)

  val productRatePlanIdMapping = mapping("ratePlanId" -> text)(ProductRatePlanId.apply)(ProductRatePlanId.unapply)

  val directDebitDataMapping = mapping(
    "account" -> text(6, 10),
    "sortcode" -> text(6, 8),
    "holder" -> text
  )(DirectDebitData.apply)(DirectDebitData.unapply)

  val creditCardDataMapping = mapping("token" -> text)(CreditCardData)(CreditCardData.unapply)

  implicit val promoCodeFormatter: Formatter[PromoCode] = new Formatter[PromoCode] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PromoCode] =
      data.get(key).filter(_.nonEmpty).map(PromoCode).toRight(Seq(FormError(key, "Cannot find a promo code")))

    override def unbind(key: String, value: PromoCode) = Map(key -> value.get)
  }

  implicit val paymentFormatter: Formatter[PaymentData] = new Formatter[PaymentData] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PaymentData] = {
      val methodKey = s"$key.type"
      data.get(methodKey).flatMap(PaymentType.fromKey) match {
        case Some(CreditCard) => creditCardDataMapping.withPrefix(key).bind(data)
        case Some(DirectDebit) => directDebitDataMapping.withPrefix(key).bind(data)
        case Some(_) => Left(Seq(FormError(methodKey, "valid values are credit-card and direct-debit")))
        case _ => Left(Seq(FormError(methodKey, "missing")))
      }
    }
    override def unbind(key: String, value: PaymentData): Map[String, String] = {
      val unPrefixed = value match {
        case CreditCardData(stripeToken) => Seq("type" -> CreditCard.toKey)
        case DirectDebitData(account, sortCode, holder) =>
          Seq(
            "account" -> account,
            "sortcode" -> sortCode,
            "holder" -> holder,
            "type" -> DirectDebit.toKey
          )
      }
      unPrefixed.map { case (k, v) => s"$key.$k" -> v }.toMap
    }
  }

  implicit val productRatePlanIdFormatter = new Formatter[ProductRatePlanId] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ProductRatePlanId] =
      Right(ProductRatePlanId(data.getOrElse(key, "")))

    override def unbind(key: String, value: ProductRatePlanId): Map[String, String] =
      Map(key -> value.get)
  }

  val subsForm = Form(mapping(
    "personal" -> personalDataMapping,
    "payment" -> of[PaymentData],
    "ratePlanId" -> of[ProductRatePlanId],
    "promoCode" -> optional(of[PromoCode])
  )(SubscriptionData.apply)(SubscriptionData.unapply)
    .verifying("DirectDebit is only available in the UK", PaymentValidation.validateDirectDebit _))

  def apply(): Form[SubscriptionData] = subsForm
}
