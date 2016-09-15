package model

import com.gu.i18n.Country.UK
import com.gu.i18n.{Country, CountryGroup, Title}
import com.gu.identity.play.IdUser
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.memsub.promo.PromoCode
import IdUserOps._
import com.gu.subscriptions.{DigipackPlan, DigitalProducts, PhysicalProducts, ProductPlan}
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
  override def toKey = "direct-debit"
}

case object CreditCard extends PaymentType {
  override def toKey = "card"
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

  lazy val currency = countryGroup.fold(CountryGroup.UK.currency)(_.currency)

  def toStringSanitized: String = s"${first.head}. $last, ${email.head}**@**${email.last}, ${address.country}"
}

case class SubscriptionData(
   personalData: PersonalData,
   paymentData: PaymentData,
   promoCode: Option[PromoCode]
)

case class DigipackData(
   plan: ProductPlan[DigitalProducts]
)

case class PaperData(
  startDate: LocalDate,
  deliveryAddress: Address,
  deliveryInstructions: Option[String],
  plan: ProductPlan[PhysicalProducts]
)

object PersonalData {
  def fromIdUser(u: IdUser) = {
    val personalData = PersonalData(
      title = u.privateFields.flatMap(_.title).flatMap(Title.fromString),
      first = u.privateFields.flatMap(_.firstName).getOrElse(""),
      last = u.privateFields.flatMap(_.secondName).getOrElse(""),
      email = u.primaryEmailAddress,
      receiveGnmMarketing = u.statusFields.flatMap(_.receiveGnmMarketing).getOrElse(false),
      address = u.billingAddress,
      telephoneNumber = u.privateFields.flatMap(_.telephoneNumber).flatMap(_.localNumber)
    )
    personalData
  }
}


case class SubscribeRequest(genericData: SubscriptionData, productData: Either[PaperData, DigipackData]) {
  def productRatePlanId = productData.fold(_.plan.productRatePlanId, _.plan.productRatePlanId)
}
