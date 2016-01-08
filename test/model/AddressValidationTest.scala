package model

import com.gu.memsub.Address
import org.specs2.mutable.Specification
import com.gu.i18n.Country._

class AddressValidationTest extends Specification {

  "AddressValidation$Test" should {
    val address = Address("lineOne", "lineTwo", "town", "", "", UK)

    "validateForCountry" should {
      "checks the postcode if required" in {
        AddressValidation.validateForCountry(address) should_=== false
        AddressValidation.validateForCountry(address.copy(postCode = "postcode")) should_=== true
      }

      "checks the subdivision if required" in {
        AddressValidation.validateForCountry(address.copy(postCode = "postcode")) should_=== true
        AddressValidation.validateForCountry(address.copy(postCode = "postcode", countyOrState = "Alaska", country = US)) should_=== true
        AddressValidation.validateForCountry(address.copy(postCode = "postcode", countyOrState = "Quebec", country = US)) should_=== false
      }
    }
  }
}
