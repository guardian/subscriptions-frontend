package model

import com.gu.i18n.CountryGroup

object GuardianWeeklyZones {
  private val allCountries = CountryGroup.allGroups.flatMap(_.countries).toSet

  val zoneACountryGroups = Set(CountryGroup.UK, CountryGroup.US)
  val zoneCCountryGroups = CountryGroup.allGroups.toSet.diff(zoneACountryGroups)

  val domesticZoneCountryGroups = Set(
    CountryGroup.UK,
    CountryGroup.US,
    CountryGroup.Canada,
    CountryGroup.US,
    CountryGroup.NewZealand,
    CountryGroup.Europe
  )
  val restOfWorldZoneCountryGroups = CountryGroup.allGroups.toSet.diff(domesticZoneCountryGroups)

  val zoneACountries = (CountryGroup.UK.countries ++ CountryGroup.US.countries).toSet
  val zoneCCountries = allCountries.diff(zoneACountries)

  val domesticZoneCountries = domesticZoneCountryGroups.flatMap(_.countries)
  val restOfWorldZoneCountries = allCountries.diff(domesticZoneCountries)
}
