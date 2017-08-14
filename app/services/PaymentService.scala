package services
import com.gu.i18n.Country.UK
import com.gu.i18n.Currency.GBP
import com.gu.i18n.{CountryGroup, Currency}
import com.gu.stripe.StripeService
import com.gu.zuora.soap.models.Commands._
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class PaymentService(val stripeService: StripeService) {

  sealed trait AccountAndPayment {
    def makeAccount: Account
    def makePaymentMethod: Future[PaymentMethod]
  }

  case class ZuoraAccountDirectDebit(paymentData: DirectDebitData, firstName: String, lastName: String, purchaserIds: PurchaserIdentifiers) extends AccountAndPayment {

    override def makeAccount = Account(purchaserIds.contactId, identityIdForAccount(purchaserIds), GBP, autopay = true, GoCardless)

    override def makePaymentMethod =
      Future(BankTransfer(
        accountNumber = paymentData.account,
        sortCode = paymentData.sortCode,
        accountHolderName = paymentData.holder,
        firstName = firstName,
        lastName = lastName,
        countryCode = UK.alpha2
      ))
  }

  class ZuoraAccountCreditCard(val paymentData: CreditCardData, val currency: Currency, val purchaserIds: PurchaserIdentifiers) extends AccountAndPayment {
    override def makeAccount = Account(purchaserIds.contactId, identityIdForAccount(purchaserIds), currency, autopay = true, Stripe)

    override def makePaymentMethod = {
      stripeService.Customer.create(description = purchaserIds.description, card = paymentData.stripeToken)
        .map(a => CreditCardReferenceTransaction(
          cardId = a.card.id,
          customerId = a.id,
          last4 = a.card.last4,
          cardCountry = CountryGroup.countryByCode(a.card.country),
          expirationMonth = a.card.exp_month,
          expirationYear = a.card.exp_year,
          cardType = a.card.`type`
        ))
    }
  }

  def identityIdForAccount(purchaserIds: PurchaserIdentifiers) = {
    purchaserIds.identityId match {
      case Some(idUser) => idUser.id
      case None => ""
    }
  }

  def makeZuoraAccountWithDirectDebit(
      paymentData: DirectDebitData,
      firstName: String,
      lastName: String,
      purchaserIds: PurchaserIdentifiers) = ZuoraAccountDirectDebit(paymentData, firstName,lastName, purchaserIds)

  def makeZuoraAccountWithCreditCard(
     paymentData: CreditCardData,
     currency: Currency,
     purchaserIds: PurchaserIdentifiers) = new ZuoraAccountCreditCard(paymentData, currency, purchaserIds)

}
