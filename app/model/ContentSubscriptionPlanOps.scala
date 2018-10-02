package model

import com.gu.i18n.Currency._
import com.gu.i18n._
import com.gu.memsub.BillingPeriod.{OneYear, Quarter, SixWeeks, Year}
import com.gu.memsub.Product
import com.gu.memsub.subsv2.CatalogPlan
import model.BillingPeriodOps._
import views.support.CountryWithCurrency

object ContentSubscriptionPlanOps {
  val weeklyUkCountries = CountryGroup.UK.copy(
    countries = List(
      Country.UK,
      Country("GG", "Guernsey"),
      Country("IM", "Isle of Man"),
      Country("JE", "Jersey")
    ))
  val weeklyZoneAGroups = List(weeklyUkCountries, CountryGroup.US)
  val weeklyZoneBGroups = {
    val rowUk = CountryGroup("Row Uk", "uk", None, CountryGroup.UK.countries.filterNot(weeklyUkCountries.countries.contains(_)), GBP, PostCode)
    rowUk :: CountryGroup.allGroups.filterNot(group => (CountryGroup.UK :: weeklyZoneAGroups) contains group)
  }
  val weeklyZoneCGroups = weeklyZoneBGroups
  val ukAndIsleOfMan = CountryGroup.UK.copy(countries = List(Country.UK, Country("IM", "Isle of Man")))


  implicit class EnrichedContentSubscriptionPlan[+A <: CatalogPlan.ContentSubscription](in: A) {

    case class LocalizationSettings(availableDeliveryCountriesWithCurrency: Option[List[CountryWithCurrency]], availableBillingCountriesWithCurrency: List[CountryWithCurrency])

    def localizationSettings: LocalizationSettings = {
      val supportedCurrencies = in.charges.currencies
      def allCountriesWithCurrencyOrGBP = CountryWithCurrency.whitelisted(supportedCurrencies, GBP)
      in.product match {
        case Product.Digipack => LocalizationSettings(None, allCountriesWithCurrencyOrGBP)

        case Product.Delivery =>
          val deliveryCountries = List(CountryWithCurrency(Country.UK, GBP))
          LocalizationSettings(Some(deliveryCountries), deliveryCountries)

        case Product.Voucher =>
          val voucherCountries = CountryWithCurrency.fromCountryGroup(ukAndIsleOfMan)
          LocalizationSettings(Some(voucherCountries), voucherCountries)

        case Product.WeeklyDomestic => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, GBP, GuardianWeeklyZones.domesticZoneCountryGroups.toList)), allCountriesWithCurrencyOrGBP)

        case Product.WeeklyZoneA => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, GBP, weeklyZoneAGroups)), allCountriesWithCurrencyOrGBP)

        case Product.WeeklyZoneB => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, USD, weeklyZoneBGroups)), allCountriesWithCurrencyOrGBP)

        case Product.WeeklyRestOfWorld => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, USD, GuardianWeeklyZones.restOfWorldZoneCountryGroups.toList)), allCountriesWithCurrencyOrGBP)

        case Product.WeeklyZoneC => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, USD, weeklyZoneCGroups)), allCountriesWithCurrencyOrGBP)
      }
    }

    def availableForCheckout: Boolean = in.charges.billingPeriod.isRecurring || in.charges.billingPeriod == SixWeeks
    def availableForRenewal: Boolean = in.charges.billingPeriod == Quarter || in.charges.billingPeriod == OneYear || in.charges.billingPeriod == Year
  }

}
