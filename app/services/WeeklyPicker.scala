package services

import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.images.{ResponsiveImageGenerator, ResponsiveImageGroup}
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
