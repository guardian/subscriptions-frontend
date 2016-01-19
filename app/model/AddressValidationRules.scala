package model

import com.gu.i18n.CountryGroup._
import com.gu.i18n.{Country, CountryGroup}

sealed trait SubdivisionRule
case object SubdivisionOptional extends SubdivisionRule
case object SubdivisionRequired extends SubdivisionRule
case class SubdivisionList(subdivision: Seq[String]) extends SubdivisionRule

case class AddressValidationRules(subdivision: SubdivisionRule)

object AddressValidationRules {
  def apply(country: Country): AddressValidationRules = CountryGroup.byCountryCode(country.alpha2) match {
    case Some(UK) => AddressValidationRules(SubdivisionOptional)
    case Some(US) => AddressValidationRules(SubdivisionList(Country.US.states))
    case Some(Canada) => AddressValidationRules(SubdivisionList(Country.Canada.states))
    case _ => AddressValidationRules(SubdivisionOptional)
  }
}
