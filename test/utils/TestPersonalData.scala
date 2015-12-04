package utils

import com.gu.i18n.{Address, Country}
import model.PersonalData

object TestPersonalData {
  val testPersonalData: PersonalData = PersonalData(
    firstName = "FirstName",
    lastName = "LastName",
    email = "email@example.com",
    receiveGnmMarketing = true,
    address = Address("address1","address2","Town", "United Kingdom", "AAAAAA", Country.UK)
  )
}
