package model

import com.gu.i18n.Country
import com.gu.memsub.Address

object AddressValidation {
  def validateForCountry(address: Address) = {
    val rules = AddressValidationRules(address.country.getOrElse(Country.UK))

    val postcodeValid = rules.postcode == PostcodeOptional || address.postCode.nonEmpty
    val subdivisionValid = rules.subdivision match {
      case SubdivisionOptional => true
      case SubdivisionRequired => address.countyOrState.nonEmpty
      case SubdivisionList(s) => s.contains(address.countyOrState)
    }

    postcodeValid && subdivisionValid
  }
}
