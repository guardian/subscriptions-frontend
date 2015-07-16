package utils

import model.{AddressData, PersonalData}

object TestPersonalData {
  val testPersonalData: PersonalData = PersonalData(
    firstName = "FirstName",
    lastName = "LastName",
    email = "email@example.com",
    receiveGnmMarketing = true,
    address = AddressData(
      address1 = "address1",
      address2 = "address2",
      town = "Town",
      postcode = "AAAAAA"
    )
  )
}
