package views.support

import com.gu.i18n
import com.gu.i18n.{Country, CountryGroup, Currency}

import scala.language.implicitConversions

sealed trait StripeServiceName {
  val jsLookupKey: String
}
case object UKStripeService extends StripeServiceName {
  val jsLookupKey = "ukPublicKey" // Referenced in assets/javascripts/modules/checkout/submit.js
}
case object AUStripeService extends StripeServiceName {
  val jsLookupKey = "auPublicKey"
}

case class CountryWithCurrency(country: i18n.Country, currency: i18n.Currency) {
  val stripeServiceName: StripeServiceName = if(country == Country.Australia) AUStripeService else UKStripeService
}

object CountryWithCurrency {

  def fromCountryGroup(countryGroup: CountryGroup): List[CountryWithCurrency] = countryGroup.countries.map(c => CountryWithCurrency(c, countryGroup.currency))

  def whitelisted(availableCurrencies: Set[Currency], default: Currency, availableCountryGroups: List[CountryGroup] = CountryGroup.allGroups): List[CountryWithCurrency] = {
    def ensureValidCurrency(group: CountryGroup) = if (availableCurrencies.contains(group.currency)) group else group.copy(currency = default)
    availableCountryGroups.map(ensureValidCurrency).flatMap(fromCountryGroup).sortBy(_.country.name)
  }

  implicit def toCountry(countryWithCurrency: CountryWithCurrency): Country = countryWithCurrency.country
}

case class CountryAndCurrencySettings(availableDeliveryCountries: Option[List[CountryWithCurrency]], availableBillingCountries: List[CountryWithCurrency], defaultCountry: Option[Country], defaultCurrency: Currency)

object DetermineCountryGroup {

  /***
    * This method takes a hint String and returns a CountryGroup if it can map to it. If the hint also implies a Country
    * which exists within the CountryGroup, then it sets the defaultCountry within that CountryGroup to that Country.
    * @param hint a CountryCode ID or a Country.alpha2 value.
    * @return a potentially modified CountryGroup
    */
  def fromHint(hint: String): Option[CountryGroup] = {
    val possibleCountryGroup = CountryGroup.byId(hint) orElse CountryGroup.byCountryCode(hint.toUpperCase)
    possibleCountryGroup.map { foundCountryGroup =>
      val determinedCountry = CountryGroup.countryByCode(hint.toUpperCase) orElse foundCountryGroup.defaultCountry
      foundCountryGroup.copy(defaultCountry = determinedCountry)
    }
  }
}
