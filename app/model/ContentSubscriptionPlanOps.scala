package model

import com.gu.i18n._
import com.gu.memsub.Product
import com.gu.memsub.subsv2.CatalogPlan
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

        case Product.WeeklyZoneA => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, GBP, weeklyZoneAGroups)), allCountriesWithCurrencyOrGBP)

        case Product.WeeklyZoneB => LocalizationSettings(Some(CountryWithCurrency.whitelisted(supportedCurrencies, USD, weeklyZoneBGroups)), allCountriesWithCurrencyOrGBP)
      }
    }
  }

}
