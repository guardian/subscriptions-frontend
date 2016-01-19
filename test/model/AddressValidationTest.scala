package model

import com.gu.memsub.Address
import org.specs2.mutable.Specification
import com.gu.i18n.Country._

class AddressValidationTest extends Specification {

  "AddressValidationTest" should {
    val address = Address("lineOne", "lineTwo", "town", "", "", UK.name)

    "validateForCountry" should {
      "checks the subdivision if required" in {
        AddressValidation.validateForCountry(address.copy(postCode = "postcode")) should_=== true
        AddressValidation.validateForCountry(address.copy(postCode = "postcode", countyOrState = "Alaska", countryName = US.name)) should_=== true
        AddressValidation.validateForCountry(address.copy(postCode = "postcode", countyOrState = "Quebec", countryName = US.name)) should_=== false
      }
    }
  }
}
