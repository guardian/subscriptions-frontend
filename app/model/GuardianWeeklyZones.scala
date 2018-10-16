package model

import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.subsv2.{CatalogPlan, WeeklyPlans}

object GuardianWeeklyZones {
  private val allCountries = CountryGroup.allGroups.flatMap(_.countries).toSet

  val zoneACountryGroups = Set(CountryGroup.UK, CountryGroup.US)

  val domesticZoneCountryGroups = Seq(
    CountryGroup.UK,
    CountryGroup.US,
    CountryGroup.Europe,
    CountryGroup.Australia,
    CountryGroup.NewZealand,
    CountryGroup.Canada
  )
  val restOfWorldZoneCountryGroups = CountryGroup.allGroups.toSet.diff(domesticZoneCountryGroups.toSet)

  val domesticZoneCountries = domesticZoneCountryGroups.flatMap(_.countries)
  val restOfWorldZoneCountries = allCountries.diff(domesticZoneCountries.toSet)

  def getDomesticCountryGroup(country: Country): Option[CountryGroup] =
    domesticZoneCountryGroups.find(countryGroup => countryGroup.countries.contains(country))
}

object PurchasableWeeklyProducts {

  sealed trait PurchasableWeeklyProduct
  case object WeeklyDomestic extends PurchasableWeeklyProduct
  case object WeeklyRestOfWorld extends PurchasableWeeklyProduct

  def plansForProduct(product: PurchasableWeeklyProduct)(implicit allWeeklyPlans: WeeklyPlans): Seq[CatalogPlan.Paid] = product match {
    case WeeklyDomestic => allWeeklyPlans.domestic.plans
    case WeeklyRestOfWorld => allWeeklyPlans.restOfWorld.plans
  }

}
