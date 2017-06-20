package services
import com.gu.i18n.Country.UK
import com.gu.i18n.{CountryGroup, Currency}
import com.gu.i18n.Currency.GBP
import com.gu.memsub.subsv2.CatalogPlan
import com.gu.salesforce.ContactId
import com.gu.stripe.StripeService
import com.gu.zuora.soap.models.Commands.{Account, BankTransfer, CreditCardReferenceTransaction, PaymentMethod}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import model._

class PaymentService(val stripeService: StripeService) {

  sealed trait Payment {
    def makeAccount: Account
    def makePaymentMethod: Future[PaymentMethod]
  }

  case class DirectDebitPayment(paymentData: DirectDebitData, firstName: String, lastName: String, purchaserIds: PurchaserIdentifiers) extends Payment {

    override def makeAccount = Account.goCardless(purchaserIds.contactId, identityIdForAccount(purchaserIds), GBP, autopay = true)

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

  class CreditCardPayment(val paymentData: CreditCardData, val currency: Currency, val purchaserIds: PurchaserIdentifiers) extends Payment {
    override def makeAccount = Account.stripe(purchaserIds.contactId, identityIdForAccount(purchaserIds), currency, autopay = true)
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

  def makeDirectDebitPayment(
      paymentData: DirectDebitData,
      firstName: String,
      lastName: String,
      purchaserIds: PurchaserIdentifiers) = DirectDebitPayment(paymentData, firstName,lastName, purchaserIds)

  def makeCreditCardPayment(
     paymentData: CreditCardData,
     currency: Currency,
     purchaserIds: PurchaserIdentifiers) = new CreditCardPayment(paymentData, currency, purchaserIds)

}
