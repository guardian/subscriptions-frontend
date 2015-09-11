package model

import com.gu.identity.play.IdUser
import com.gu.membership.zuora.{Countries, Address}

case class PaymentData(account: String, sortCodeValue: String, holder: String) {
  val sortCode = sortCodeValue.filter(_.isDigit)
}

case class PersonalData(firstName: String, lastName: String, email: String, receiveGnmMarketing: Boolean, address: Address) {
  def fullName = s"$firstName $lastName"
}

case class SubscriptionData(personalData: PersonalData, paymentData: PaymentData, ratePlanId: String)

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
      country = Countries.UK
    )

    val personalData = PersonalData(
      u.privateFields.getOrBlank(_.firstName),
      u.privateFields.getOrBlank(_.secondName),
      u.primaryEmailAddress,
      u.statusFields.getOrDefault(_.receiveGnmMarketing, false),
      addressData
    )

    val blankPaymentData = PaymentData("", "", "")

    val blankRatePlanId = ""

    SubscriptionData(personalData, blankPaymentData, blankRatePlanId)
  }
}
