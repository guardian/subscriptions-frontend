package services

import com.gu.i18n.{Country, Currency, GBP}
import com.gu.memsub.BillingPeriod
import com.gu.salesforce.ContactId
import com.gu.stripe.StripeService
import com.gu.subscriptions.DigipackPlan
import com.gu.zuora.soap.models.Commands.{Account, BankTransfer, CreditCardReferenceTransaction, PaymentMethod}
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait PaymentService {
  def stripeService: StripeService

  sealed trait Payment {
    def makeAccount: Account
    def makePaymentMethod: Future[PaymentMethod]
  }

  case class DirectDebitPayment(paymentData: DirectDebitData, personalData: PersonalData, memberId: ContactId) extends Payment {
    override def makeAccount = Account.goCardless(memberId, GBP, autopay = true)

    override def makePaymentMethod =
      Future(BankTransfer(
        accountNumber = paymentData.account,
        sortCode = paymentData.sortCode,
        accountHolderName = paymentData.holder,
        firstName = personalData.first,
        lastName = personalData.last,
        countryCode = Country.UK.alpha2
      ))
  }

  class CreditCardPayment(val paymentData: CreditCardData, val currency: Currency, val purchaserIds: PurchaserIdentifiers) extends Payment {
    override def makeAccount = Account.stripe(purchaserIds.memberId, currency, autopay = true)
    override def makePaymentMethod = {
      stripeService.Customer.create(description = purchaserIds.description, card = paymentData.stripeToken)
        .map(a => CreditCardReferenceTransaction(a.card.id, a.id))
    }
  }

  def makeDirectDebitPayment(paymentData: DirectDebitData, personalData: PersonalData, memberId: ContactId) = {
    require(personalData.country == Country.UK, "Direct Debit payment only works in the UK right now")
    new DirectDebitPayment(paymentData, personalData, memberId)
  }

  def makeCreditCardPayment(
     paymentData: CreditCardData,
     personalData: PersonalData,
     purchaserIds: PurchaserIdentifiers,
     plan: DigipackPlan[BillingPeriod]) = {
    val desiredCurrency = personalData.currency
    val currency = if (plan.currencies.contains(desiredCurrency)) desiredCurrency else GBP

    new CreditCardPayment(paymentData, currency, purchaserIds)
  }
}
