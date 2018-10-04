package services

import com.gu.i18n.{Country, CountryGroup}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import WeeklyPicker._
import model.PurchasableWeeklyProducts.{WeeklyDomestic, WeeklyRestOfWorld, WeeklyZoneA, WeeklyZoneC}
import org.joda.time.{DateTime, DateTimeZone}

class WeeklyPickerTest extends Specification with Mockito {

  val countryInZoneA = Country.US
  val countryInZoneC = Country.Australia
  val countryinDomesticZone = Country.Australia
  val countryInRestOfWorldZone = Country("ER", "Eritrea")

  "isInRestOfWorldOrZoneC" should {
    "return true for a country that is in both rest of world and zone C" in {
      isInRestOfWorldOrZoneC(countryInRestOfWorldZone, true) shouldEqual true
      isInRestOfWorldOrZoneC(countryInRestOfWorldZone, false) shouldEqual true
    }

    "return for false for Australia when updated prices are shown, and true for old prices" in {
      //australia is Zone C, but moves to domestic with the price updates
      isInRestOfWorldOrZoneC(countryinDomesticZone, true) shouldEqual false
      isInRestOfWorldOrZoneC(countryInZoneC, false) shouldEqual true
    }

    "return false for countries that are in zone A and the domestic zone regardless of updated price switch" in {
      isInRestOfWorldOrZoneC(countryInZoneA, true) shouldEqual false
      isInRestOfWorldOrZoneC(countryInZoneA, false) shouldEqual false
    }
  }

  "showUpdatedPricesShould" should {

    val switchoverTimeInPast = DateTime.parse("2018-09-10T09:45:00").withZone(DateTimeZone.UTC)
    val switchoverTimeInFuture = DateTime.parse("2038-09-10T09:45:00").withZone(DateTimeZone.UTC)

    "return true if the relevant query string is set, regardless of the time" in {
      showUpdatedPrices(true, switchoverTimeInPast) shouldEqual true
      showUpdatedPrices(true, switchoverTimeInFuture) shouldEqual true
    }

    "return true if the relevant query string is not set, and the switchover time is in the past" in {
      showUpdatedPrices(false, switchoverTimeInPast) shouldEqual true
    }

    "return false if the relevant query string is not set, and the switchover time is in the future" in {
      showUpdatedPrices(false, switchoverTimeInFuture) shouldEqual false
    }

  }

  "product" should {

    "select WeeklyDomestic if the country is US and showUpdatedPrices is set to true" in {
      product(Country.US, true) shouldEqual WeeklyDomestic
    }

    "select WeeklyZoneA if the country is US and showUpdatedPrices is set to false" in {
      product(Country.US, false) shouldEqual WeeklyZoneA
    }

    "select WeeklyRestOfWorld if the country is Eritrea and showUpdatedPrices is set to true" in {
      product(Country("ER", "Eritrea"), true) shouldEqual WeeklyRestOfWorld
    }

    "select WeeklyZoneC if the country is Eritrea and showUpdatedPrices is set to false" in {
      product(Country("ER", "Eritrea"), false) shouldEqual WeeklyZoneC
    }

  }

  "productForCountryGroup" should {

    "select WeeklyDomestic if the country group is EU and showUpdatedPrices is set to true" in {
      productForCountryGroup(CountryGroup.Europe, true) shouldEqual WeeklyDomestic
    }

    "select WeeklyZoneC if the country group is EU and showUpdatedPrices is set to false" in {
      productForCountryGroup(CountryGroup.Europe, false) shouldEqual WeeklyZoneC
    }

  }

}
