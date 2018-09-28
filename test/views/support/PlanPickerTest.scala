package views.support

import com.gu.i18n.Country
import com.gu.memsub.subsv2._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class PlanPickerTest extends Specification with Mockito {

  val countryInZoneA = Country.US
  val countryInZoneC = Country.Australia
  val countryinDomesticZone = Country.Australia
  val countryInRestOfWorldZone = Country("ER", "Eritrea")

  implicit val mockCatalog = mock[Catalog]
  val mockWeeklyPlans = mock[WeeklyPlans]
  val mockWeeklyZoneAPlans = mock[WeeklyZoneAPlans]
  val mockWeeklyZoneBPlans = mock[WeeklyZoneBPlans]
  val mockWeeklyZoneCPlans = mock[WeeklyZoneCPlans]
  val mockWeeklyDomesticPlans = mock[WeeklyDomesticPlans]
  val mockWeeklyRestOfWorldPlans = mock[WeeklyRestOfWorldPlans]

  mockWeeklyPlans.zoneA returns mockWeeklyZoneAPlans
  mockWeeklyPlans.zoneB returns mockWeeklyZoneBPlans
  mockWeeklyPlans.zoneC returns mockWeeklyZoneCPlans
  mockWeeklyPlans.domestic returns mockWeeklyDomesticPlans
  mockWeeklyPlans.restOfWorld returns mockWeeklyRestOfWorldPlans

  mockWeeklyZoneAPlans.plans returns Nil
  mockWeeklyZoneBPlans.plans returns Nil
  mockWeeklyZoneCPlans.plans returns Nil
  mockWeeklyDomesticPlans.plans returns Nil
  mockWeeklyRestOfWorldPlans.plans returns Nil

  mockCatalog.weekly returns mockWeeklyPlans

  mockCatalog.weekly.restOfWorld returns mockWeeklyRestOfWorldPlans


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

  "restOfWorldOrZoneCPlans" should {
    "return the rest of world plans when updated prices are switched on" in {
      PlanPicker.restOfWorldOrZoneCPlans(true) shouldEqual mockWeeklyRestOfWorldPlans.plans
    }

    "return the zone C plans when updated prices are switched off" in {
      PlanPicker.restOfWorldOrZoneCPlans(false) shouldEqual mockWeeklyZoneCPlans.plans
    }
  }

}
