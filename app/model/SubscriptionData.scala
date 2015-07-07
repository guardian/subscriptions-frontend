package model

case class PaymentData(account: String, sortCode1: String, sortCode2: String, sortCode3: String, holder: String) {
  val sortCode = s"$sortCode1$sortCode2$sortCode3"
}
case class AddressData(address1: String, address2: String, town: String, postcode: String) {
  def asString =
    List(address1, address2, town, postcode).filterNot(_.isEmpty).mkString(", ")
}

case class PersonalData(firstName: String, lastName: String, email: String, address: AddressData) {
  def fullName = s"$firstName $lastName"
}

case class SubscriptionData(personalData: PersonalData, paymentData: PaymentData)
