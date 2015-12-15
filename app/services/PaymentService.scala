package services

import com.gu.i18n.{GBP, Country}
import com.gu.membership.salesforce.ContactId
import com.gu.membership.stripe.StripeService
import com.gu.membership.zuora.soap.actions.subscribe._
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
        firstName = personalData.firstName,
        lastName = personalData.lastName,
        countryCode = Country.UK.alpha2
      ))
  }

  class CreditCardPayment(val paymentData: CreditCardData, val userIdData: UserIdData, val memberId: ContactId) extends Payment {
    override def makeAccount = Account.stripe(memberId, GBP, autopay = true)
    override def makePaymentMethod =
      stripeService.Customer.create(userIdData.id.id, paymentData.stripeToken)
        .map(CreditCardReferenceTransaction)
  }

  def makeDirectDebitPayment(paymentData: DirectDebitData, personalData: PersonalData, memberId: ContactId) =
    new DirectDebitPayment(paymentData, personalData, memberId)

  def makeCreditCardPayment(paymentData: CreditCardData, userIdData: UserIdData, memberId: ContactId) =
    new CreditCardPayment(paymentData, userIdData, memberId = memberId)
}
