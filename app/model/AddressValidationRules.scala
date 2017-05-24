package model

import com.gu.i18n.CountryGroup._
import com.gu.i18n.{Country, CountryGroup}

sealed trait SubdivisionRule
case object SubdivisionOptional extends SubdivisionRule
case object SubdivisionRequired extends SubdivisionRule
case class SubdivisionList(subdivision: Seq[String]) extends SubdivisionRule

sealed trait PostcodeRule
case object PostcodeOptional extends PostcodeRule
case object PostcodeRequired extends PostcodeRule

case class AddressValidationRules(postcode: PostcodeRule,
                                  subdivision: SubdivisionRule)

object AddressValidationRules {
  def apply(country: Country): AddressValidationRules = CountryGroup.byCountryCode(country.alpha2) match {
    case Some(UK) => AddressValidationRules(PostcodeRequired, SubdivisionOptional)
    case Some(US) => AddressValidationRules(PostcodeRequired, SubdivisionList(Country.US.states))
    case Some(Canada) => AddressValidationRules(PostcodeRequired, SubdivisionList(Country.Canada.states))
    case Some(Australia) => AddressValidationRules(PostcodeRequired, SubdivisionList(Country.Australia.states))
    case _ => AddressValidationRules(PostcodeOptional, SubdivisionOptional)
  }
}
