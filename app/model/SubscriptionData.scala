package model

import com.gu.i18n.CountryGroup
import com.gu.identity.play.IdUser
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.{Address, FullName}

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
                        address: Address
                        ) extends FullName {
  def fullName = s"$first $last"

  lazy val currency = {
    val code = address.countryCode
    CountryGroup.byCountryCode(code)
      .map(_.currency)
      .getOrElse(throw new NoSuchElementException(s"Could not find a country group with code $code"))
  }

}

case class SubscriptionData(personalData: PersonalData, paymentData: PaymentData, productRatePlanId: ProductRatePlanId)

object SubscriptionData {
  def fromIdUser(u: IdUser) = {
    implicit class OptField[A](opt: Option[A]) {
      def getOrDefault[B](get: A => Option[B], default: B): B =
        (for {
          fieldOpt <- opt
          fieldValue <- get(fieldOpt)
        } yield fieldValue) getOrElse default
      def getOrBlank(get: A => Option[String]): String = getOrDefault(get, "")
    }

    val addressData = Address(
      lineOne = u.privateFields.getOrBlank(_.billingAddress1),
      lineTwo = u.privateFields.getOrBlank(_.billingAddress2),
      town = u.privateFields.getOrBlank(_.billingAddress3),
      postCode = u.privateFields.getOrBlank(_.billingPostcode),
      countyOrState = u.privateFields.getOrBlank(_.country),
      countryCode = u.privateFields.getOrBlank(_.country)
    )

    val personalData = PersonalData(
      u.privateFields.getOrBlank(_.firstName),
      u.privateFields.getOrBlank(_.secondName),
      u.primaryEmailAddress,
      u.statusFields.getOrDefault(_.receiveGnmMarketing, false),
      addressData
    )

    val blankPaymentData = DirectDebitData("", "", "")

    val blankRatePlanId = ProductRatePlanId("") // this makes me very nervous indeed

    SubscriptionData(personalData, blankPaymentData, blankRatePlanId)
  }
}
