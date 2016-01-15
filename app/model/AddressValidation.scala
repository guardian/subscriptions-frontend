package model

import com.gu.memsub.Address

object AddressValidation {
  def validateForCountry(address: Address) = {
    val rules = AddressValidationRules(address.country)

    val postcodeValid = rules.postcode == PostcodeOptional || address.postCode.nonEmpty
    val subdivisionValid = rules.subdivision match {
      case SubdivisionOptional => true
      case SubdivisionRequired => address.countyOrState.nonEmpty
      case SubdivisionList(s) => s.contains(address.countyOrState)
    }

    postcodeValid && subdivisionValid
  }
}
