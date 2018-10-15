package services

import com.gu.i18n.{Country, CountryGroup}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import WeeklyPicker._
import model.PurchasableWeeklyProducts.{WeeklyDomestic, WeeklyRestOfWorld, WeeklyZoneA, WeeklyZoneC}
import org.joda.time.{DateTime, DateTimeZone}

class WeeklyPickerTest extends Specification with Mockito {

  val countryinDomesticZone = Country.Australia
  val countryInRestOfWorldZone = Country("ER", "Eritrea")

  "isInRestOfWorldOrZoneC" should {
    "return true for a country that is in rest of world" in {
      isInRestOfWorld(countryInRestOfWorldZone) shouldEqual true
    }

    "return false for countries that are in the domestic zone" in {
      isInRestOfWorld(countryinDomesticZone) shouldEqual false
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

    "select WeeklyDomestic if the country is US" in {
      product(Country.US) shouldEqual WeeklyDomestic
    }

    "select WeeklyRestOfWorld if the country is Eritrea" in {
      product(Country("ER", "Eritrea")) shouldEqual WeeklyRestOfWorld
    }

  }

  "productForCountryGroup" should {

    "select WeeklyDomestic if the country group is EU" in {
      productForCountryGroup(CountryGroup.Europe) shouldEqual WeeklyDomestic
    }

  }

}
