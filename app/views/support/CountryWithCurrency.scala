package views.support

import com.gu.i18n
import com.gu.i18n.{Country, CountryGroup, Currency}

import scala.language.implicitConversions

case class CountryWithCurrency(country: i18n.Country, currency: i18n.Currency)

object CountryWithCurrency {
  val all = i18n.CountryGroup.allGroups.flatMap(fromCountryGroup).sortBy(_.country.name)

  def withCurrency(c: Currency) = all.map(_.copy(currency = c))

  def fromCountryGroup(countryGroup: CountryGroup): List[CountryWithCurrency] = countryGroup.countries.map(c => CountryWithCurrency(c, countryGroup.currency))

  def whitelisted(availableCurrencies: Set[Currency], default: Currency, availableCountryGroups: List[CountryGroup] = CountryGroup.allGroups): List[CountryWithCurrency] = {
    def ensureValidCurrency(group: CountryGroup) = if (availableCurrencies.contains(group.currency)) group else group.copy(currency = default)
    availableCountryGroups.map(ensureValidCurrency).flatMap(fromCountryGroup).sortBy(_.country.name)
  }

  implicit def toCountry(countryWithCurrency: CountryWithCurrency): Country = countryWithCurrency.country
}

case class CountryAndCurrencySettings(availableDeliveryCountries: Option[List[CountryWithCurrency]], availableBillingCountries: List[CountryWithCurrency], defaultCountry: Option[Country], defaultCurrency: Currency)
