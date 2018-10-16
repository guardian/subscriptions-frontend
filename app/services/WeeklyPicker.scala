package services

import com.gu.i18n.{Country, CountryGroup}
import model.GuardianWeeklyZones
import model.PurchasableWeeklyProducts._

object WeeklyPicker {

  import model.GuardianWeeklyZones._

  def isInRestOfWorld(country: Country): Boolean = GuardianWeeklyZones.restOfWorldZoneCountries.contains(country)

  def product(country: Country): PurchasableWeeklyProduct = {
    if (domesticZoneCountries.contains(country)) WeeklyDomestic
    else WeeklyRestOfWorld
  }

  def productForCountryGroup(countryGroup: CountryGroup): PurchasableWeeklyProduct = {
    if (domesticZoneCountryGroups.contains(countryGroup)) WeeklyDomestic
    else WeeklyRestOfWorld
  }

}
