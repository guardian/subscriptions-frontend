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

object ImagePicker {

  private val guardianWeeklyHeaderId = "c7c76ffe9b2abe16b5d914dd7a9a23db9b32840b/0_0_14400_1680"
  private val guardianWeeklyRedesignHeaderId = "c933375535e24a9fd3c2befac96a5fafaaed6f4f/0_0_9985_1165"

  private val guardianWeeklyPackshotId = "987daf55251faf1637f92bffa8aa1eeec8de72b5/0_0_1670_1558"
  private val guardianWeeklyRedesignPackshotId = "4e2eaacb68f29b9573c015c248134dc1614d0fa3/0_0_2155_2800"

  def defaultHeaderImage(rawQueryString: String = ""): ResponsiveImageGroup = {
    WeeklyPicker.showUpdatedPrices(WeeklyPicker.forceShowNewPricing(rawQueryString)) match {
      case true => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyRedesignHeaderId,Seq(2000), "png"),
        altText = Some("Selection of Guardian Weekly covers")
      )
      case false => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyHeaderId,Seq(2000), "jpg"),
        altText = Some("Selection of Guardian Weekly covers")
      )
    }

  }

  def defaultPackshotImage(rawQueryString: String = ""): ResponsiveImageGroup = {
    WeeklyPicker.showUpdatedPrices(WeeklyPicker.forceShowNewPricing(rawQueryString)) match {
      case true => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyRedesignPackshotId, Seq(385), "png"),
        altText = Some("A Guardian Weekly cover")
      )
      case false => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyPackshotId, Seq(500, 1000), "png"),
        altText = Some("A Guardian Weekly cover")
      )
    }

  }

}
