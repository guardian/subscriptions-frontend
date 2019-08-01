package model
import com.gu.i18n.Country.UK
import com.gu.i18n.{CountryGroup, Currency, Title}
import com.gu.identity.model.{User => IdUser}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.subsv2._
import model.IdUserOps._
import org.joda.time.LocalDate

sealed trait PaymentType {
  def toKey: String
}

object PaymentType {
  def fromKey(key: String): Option[PaymentType] = key match {
    case "card" => Some(CreditCard)
    case "direct-debit" => Some(DirectDebit)
    case _ => None
  }
}

case object DirectDebit extends PaymentType {
  override val toKey = "direct-debit"
}

case object CreditCard extends PaymentType {
  override val toKey = "card"
}

sealed trait PaymentData

case class DirectDebitData(account: String, sortCodeValue: String, holder: String) extends PaymentData {
  val sortCode: String = sortCodeValue.filter(_.isDigit)
}
case class CreditCardData(stripeToken: String) extends PaymentData

case class PersonalData(first: String,
                        last: String,
                        email: String,
                        receiveGnmMarketing: Boolean,
                        address: Address,
                        telephoneNumber: Option[String] = None,
                        title: Option[Title] = None
                        ) extends FullName {

  def fullName = s"$first $last"

  private lazy val countryGroup = CountryGroup.byCountryNameOrCode(address.country.fold(UK.alpha2)(c => c.alpha2))

  def toStringSanitized: String = s"${first.head}. $last, ${email.head}**@**${email.last}, ${address.country}"
}

case class SubscriptionData(
   personalData: PersonalData,
   paymentData: PaymentData,
   promoCode: Option[PromoCode],
   currency: Currency,
   ophanData: OphanData
)

case class DigipackData(
   plan: CatalogPlan.Digipack[BillingPeriod]
)

case class PaperData(
  startDate: LocalDate,
  deliveryRecipient: DeliveryRecipient,
  deliveryInstructions: Option[String],
  plan: CatalogPlan.Paper
) {
  val sanitizedDeliveryInstructions: Option[String] = deliveryInstructions.map(instructions => instructions.replaceAll("\"", ""))
}

object PersonalData {
  def fromIdUser(u: IdUser): PersonalData = {
    val phoneNumber: Option[String] = for {
      identityPhoneNumber <- u.privateFields.telephoneNumber
      countryCode <- identityPhoneNumber.countryCode
      localNumber <- identityPhoneNumber.localNumber
    } yield NormalisedTelephoneNumber(countryCode, localNumber).format

    val personalData = PersonalData(
      title = u.privateFields.title.flatMap(Title.fromString),
      first = u.privateFields.firstName.mkString,
      last = u.privateFields.secondName.mkString,
      email = u.primaryEmailAddress,
      receiveGnmMarketing = false, // Deprecated, unused and not a GDPR compliant consent
      address = u.billingAddress,
      telephoneNumber = phoneNumber
    )
    personalData
  }
}


case class SubscribeRequest(genericData: SubscriptionData, productData: Either[PaperData, DigipackData]) {
  def productRatePlanId: ProductRatePlanId = productData.fold(_.plan.id, _.plan.id)
}

case class DeliveryRecipient(title: Option[Title], firstName: Option[String], lastName: Option[String], email: Option[String], address: Address) extends FullName {
  val first: String = firstName.mkString
  val last: String = lastName.mkString
  val isGiftee: Boolean = (Seq() ++ title.map(_.title) ++ firstName ++ lastName ++ email).exists(_.trim.nonEmpty) // TODO make this a field of the case class
  val gifteeAddress: Option[Address] = if (isGiftee) Some(address) else None
  val buyersMailingAddress: Option[Address] = if (!isGiftee) Some(address) else None
}
