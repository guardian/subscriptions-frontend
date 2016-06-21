package model

import com.gu.i18n.Country

object PaymentValidation {
  def validateDirectDebit(subscriptionData: SubsFormData): Boolean = {
    subscriptionData.paymentData match {
      case _: DirectDebitData =>
        subscriptionData.personalData.address.country.contains(Country.UK)
      case _ => true
    }
  }
}
