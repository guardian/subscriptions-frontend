package views.support

import com.gu.i18n.{Country, CountryGroup}
import model.GuardianWeeklyZones
import org.specs2.mutable.Specification

class WeeklyPromotionTest extends Specification {

  //On the GW page, if we get a requestCountry, it will be first in the list
  //If this would cause a duplicate, we remove it.
  "domesticCountryGroupsToDisplay" should {

    val countryInRestOfWorldZone = Country("ER", "Eritrea")
    val countryInAustraliaCountryGroup = Country("KI", "Kiribati")
    val countryInEuropeCountryGroup = Country("FR", "France")

    "return all of the domestic regions when the request country is rest of world" in {
      val domesticPlansToDisplay = WeeklyPromotion.domesticCountryGroupsToDisplay(countryInRestOfWorldZone)
      domesticPlansToDisplay must contain (GuardianWeeklyZones.domesticZoneCountryGroups.toSet)
    }

    "return all other domestic regions other than US when the request country is US" in {
      val domesticPlansToDisplay = WeeklyPromotion.domesticCountryGroupsToDisplay(Country.US)
      val expected = GuardianWeeklyZones.domesticZoneCountryGroups.toSet.filterNot(_ == CountryGroup.US)
      domesticPlansToDisplay.toSet mustEqual (expected)
    }

    "still return all other domestic regions when the request country is in the Australia CountryGroup" in {
      val domesticPlansToDisplay = WeeklyPromotion.domesticCountryGroupsToDisplay(countryInAustraliaCountryGroup)
      domesticPlansToDisplay must contain (GuardianWeeklyZones.domesticZoneCountryGroups.toSet)
    }

    "still return all domestic regions when the request country is in Europe" in {
      val domesticPlansToDisplay = WeeklyPromotion.domesticCountryGroupsToDisplay(countryInEuropeCountryGroup)
      domesticPlansToDisplay must contain (GuardianWeeklyZones.domesticZoneCountryGroups.toSet)
    }

  }
}
