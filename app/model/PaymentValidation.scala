package model

import com.gu.i18n.Country

object PaymentValidation {
  def validateDirectDebit(subscriptionData: SubscriptionData): Boolean = {
    subscriptionData.paymentData match {
      case _: DirectDebitData =>
        subscriptionData.personalData.country == Country.UK
      case _ => true
    }
  }
}
