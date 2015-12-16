package utils

import com.gu.i18n.Country
import com.gu.memsub.Address
import model.PersonalData

object TestPersonalData {
  val testPersonalData: PersonalData = PersonalData(
    first = "FirstName",
    last = "LastName",
    email = "email@example.com",
    receiveGnmMarketing = true,
    address = Address("address1","address2","Town", "United Kingdom", "AAAAAA", Country.UK)
  )
}
