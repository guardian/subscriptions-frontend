package views.support

import com.gu.i18n.Country
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class PlanPickerTest extends Specification with Mockito {

  val countryInZoneA = Country.US
  val countryInZoneC = Country.Australia
  val countryinDomesticZone = Country.Australia
  val countryInRestOfWorldZone = Country("ER", "Eritrea")

  "isInRestOfWorldOrZoneC" should {
    "return true for a country that is in both rest of world and zone C" in {
      PlanPicker.isInRestOfWorldOrZoneC(countryInRestOfWorldZone, true) shouldEqual true
      PlanPicker.isInRestOfWorldOrZoneC(countryInRestOfWorldZone, false) shouldEqual true
    }

    "return for false for Australia when updated prices are shown, and true for old prices" in {
      //australia is Zone C, but moves to domestic with the price updates
      PlanPicker.isInRestOfWorldOrZoneC(countryinDomesticZone, true) shouldEqual false
      PlanPicker.isInRestOfWorldOrZoneC(countryInZoneC, false) shouldEqual true
    }

    "return false for countries that are in zone A and the domestic zone regardless of updated price switch" in {
      PlanPicker.isInRestOfWorldOrZoneC(countryInZoneA, true) shouldEqual false
      PlanPicker.isInRestOfWorldOrZoneC(countryInZoneA, false) shouldEqual false
    }
  }

}
