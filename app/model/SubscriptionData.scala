package model

case class PaymentData(account: String, sortCode1: String, sortCode2: String, sortCode3: String, holder: String) {
  val sortCode = s"$sortCode1$sortCode2$sortCode3"
}
case class AddressData(house: String, street: String, town: String, postcode: String)

case class PersonalData(firstName: String, lastName: String, email: String, address: AddressData)

case class SubscriptionData(personalData: PersonalData, paymentData: PaymentData)
