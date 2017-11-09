package model
import com.gu.i18n.Country.UK
import com.gu.i18n.{CountryGroup, Currency, Title}
import com.gu.identity.play.IdUser
import com.gu.memsub._
import com.gu.memsub.promo.PromoCode
import IdUserOps._
import org.joda.time.LocalDate
import com.gu.memsub.subsv2._

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
  val sortCode = sortCodeValue.filter(_.isDigit)
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
  deliveryAddress: Address,
  deliveryInstructions: Option[String],
  plan: CatalogPlan.Paper
) {
  val sanitizedDeliveryInstructions = deliveryInstructions.map(instructions => instructions.replaceAll("\"", ""))
}

object PersonalData {
  def fromIdUser(u: IdUser) = {
    val phoneNumber: Option[String] = for {
      identityPhoneNumber <- u.privateFields.flatMap(_.telephoneNumber)
      countryCode <- identityPhoneNumber.countryCode
      localNumber <- identityPhoneNumber.localNumber
    } yield NormalisedTelephoneNumber(countryCode, localNumber).format

    val personalData = PersonalData(
      title = u.privateFields.flatMap(_.title).flatMap(Title.fromString),
      first = u.privateFields.flatMap(_.firstName).getOrElse(""),
      last = u.privateFields.flatMap(_.secondName).getOrElse(""),
      email = u.primaryEmailAddress,
      receiveGnmMarketing = u.statusFields.flatMap(_.receiveGnmMarketing).getOrElse(false),
      address = u.billingAddress,
      telephoneNumber = phoneNumber
    )
    personalData
  }
}


case class SubscribeRequest(genericData: SubscriptionData, productData: Either[PaperData, DigipackData]) {
  def productRatePlanId = productData.fold(_.plan.id, _.plan.id)
}
