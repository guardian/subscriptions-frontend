package model

import com.gu.i18n.{Country, Currency, GBP}

object PaymentValidation {
  def validateDirectDebit(subscriptionData: SubscriptionData): Boolean = {
    subscriptionData.paymentData match {
      case _: DirectDebitData =>
        subscriptionData.personalData.address.country.contains(Country.UK) && subscriptionData.currency == GBP
      case _ => true
    }
  }
}
