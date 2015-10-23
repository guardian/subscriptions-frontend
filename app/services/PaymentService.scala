package services

import com.gu.membership.salesforce.MemberId
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

  case class DirectDebitPayment(paymentData: DirectDebitData, personalData: PersonalData, memberId: MemberId) extends Payment {
    override lazy val makeAccount = Account.goCardless(memberId, autopay = true)

    override lazy val makePaymentMethod =
      Future(BankTransfer(
        accountNumber = paymentData.account,
        sortCode = paymentData.sortCode,
        accountHolderName = paymentData.holder,
        firstName = personalData.firstName,
        lastName = personalData.lastName))
  }

  class CreditCardPayment(val paymentData: CreditCardData, val userIdData: UserIdData, val memberId: MemberId) extends Payment {
    override val makeAccount = Account.stripe(memberId, autopay = true)
    override lazy val makePaymentMethod =
      stripeService.Customer.create(userIdData.id.id, paymentData.stripeToken)
        .map(CreditCardReferenceTransaction)
  }

  def makeDirectDebitPayment(paymentData: DirectDebitData, personalData: PersonalData, memberId: MemberId) =
    new DirectDebitPayment(paymentData, personalData, memberId)

  def makeCreditCardPayment(paymentData: CreditCardData, userIdData: UserIdData, memberId: MemberId) =
    new CreditCardPayment(paymentData, userIdData, memberId = memberId)
}
