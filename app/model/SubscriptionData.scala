package model

import com.gu.identity.play.IdUser

case class PaymentData(account: String, sortCode1: String, sortCode2: String, sortCode3: String, holder: String) {
  val sortCode = s"$sortCode1$sortCode2$sortCode3"
}

case class AddressData(address1: String, address2: String, town: String, postcode: String) {
  def asString =
    List(address1, address2, town, postcode).filterNot(_.isEmpty).mkString(", ")
}

case class PersonalData(firstName: String, lastName: String, email: String, receiveGnmMarketing: Boolean, address: AddressData) {
  def fullName = s"$firstName $lastName"
}

case class SubscriptionData(personalData: PersonalData, paymentData: PaymentData)
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

    val addressData = AddressData(
      u.privateFields.getOrBlank(_.billingAddress1),
      u.privateFields.getOrBlank(_.billingAddress2),
      u.privateFields.getOrBlank(_.billingAddress3),
      u.privateFields.getOrBlank(_.billingPostcode)
    )

    val personalData = PersonalData(
      u.privateFields.getOrBlank(_.firstName),
      u.privateFields.getOrBlank(_.secondName),
      u.primaryEmailAddress,
      u.statusFields.getOrDefault(_.receiveGnmMarketing, false),
      addressData
    )

    val blankPaymentData = PaymentData("", "", "", "", "")

    SubscriptionData(personalData, blankPaymentData)
  }
}
