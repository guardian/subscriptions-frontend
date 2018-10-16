package services

import com.gu.i18n.{Country, CountryGroup}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import WeeklyPicker._
import model.PurchasableWeeklyProducts.{WeeklyDomestic, WeeklyRestOfWorld}

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
