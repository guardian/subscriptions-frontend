package services

import com.gu.i18n.{Country, CountryGroup}
import model.GuardianWeeklyZones
import model.PurchasableWeeklyProducts._
import org.joda.time.{DateTime, DateTimeZone}

object WeeklyPicker {

  import model.GuardianWeeklyZones._

  def forceShowNewPricing(rawQueryString: String) = rawQueryString.contains("gwoct18")

  val updateTime = DateTime.parse("2018-10-10T09:45:00Z")

  def showUpdatedPrices(forceShowUpdatedPrices: Boolean, timeToUpdate: DateTime = updateTime): Boolean = {
    val now = DateTime.now().withZone(DateTimeZone.UTC)
    now.isAfter(timeToUpdate) || forceShowUpdatedPrices
  }

  def isInRestOfWorldOrZoneC(country: Country, showUpdatedPrices: Boolean): Boolean = {
    if (showUpdatedPrices) GuardianWeeklyZones.restOfWorldZoneCountries.contains(country)
    else GuardianWeeklyZones.zoneCCountries.contains(country)
  }

  def restOfWorldOrZoneC(showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) WeeklyRestOfWorld
    else WeeklyZoneC
  }

  def product(country: Country, showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) {
      if (domesticZoneCountries.contains(country)) WeeklyDomestic
      else WeeklyRestOfWorld
    }
    else {
      if (zoneACountries.contains(country)) WeeklyZoneA
      else WeeklyZoneC
    }
  }

  def productForCountryGroup(countryGroup: CountryGroup, showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) {
      if (domesticZoneCountryGroups.contains(countryGroup)) WeeklyDomestic
      else WeeklyRestOfWorld
    }
    else {
      if (zoneACountryGroups.contains(countryGroup)) WeeklyZoneA
      else WeeklyZoneC
    }
  }

}
