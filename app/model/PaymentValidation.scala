package model

import com.gu.i18n.Country.{Australia, UK, US}
import com.gu.i18n.Currency._
import com.gu.i18n.{Country, Currency}
import com.gu.memsub.Product
import com.gu.memsub.subsv2.CatalogPlan
import views.support.CountryWithCurrency

object PaymentValidation {

  private val nonOverridableCountries = Seq(UK, US, Australia)

  def validateDirectDebit(subscriptionData: SubscriptionData): Boolean = {
    subscriptionData.paymentData match {
      case _: DirectDebitData =>
        subscriptionData.personalData.address.country.contains(UK) && subscriptionData.currency == GBP
      case _ => true
    }
  }

  def validateCurrency(currency: Currency, settings: CountryWithCurrency, plan: CatalogPlan.ContentSubscription): Boolean = {

    def isValidCurrencyOverride = plan.product match {
      case Product.WeeklyZoneC => currency == GBP && !nonOverridableCountries.contains(settings.country)
      case _ => false
    }

    settings.currency == currency || isValidCurrencyOverride
  }
}
