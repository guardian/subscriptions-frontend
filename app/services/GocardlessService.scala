package services

import com.gocardless.GoCardlessClient
import com.gocardless.GoCardlessClient.Environment.{LIVE, SANDBOX}
import com.gocardless.errors.GoCardlessApiException
import com.gocardless.resources.BankDetailsLookup.AvailableDebitScheme
import com.typesafe.scalalogging.LazyLogging
import model.DirectDebitData
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import touchpoint.GoCardlessToken

import scala.concurrent.Future

class GoCardlessService(goCardlessToken: GoCardlessToken) extends LazyLogging {

  lazy val client: GoCardlessClient = GoCardlessClient.create(goCardlessToken.token, if (goCardlessToken.isProdToken) LIVE else SANDBOX)

  def mandatePDFUrl(paymentData: DirectDebitData): Future[String] =
    Future {
      client.mandatePdfs().create()
        .withAccountHolderName(paymentData.holder)
        .withAccountNumber(paymentData.account)
        .withBranchCode(paymentData.sortCode)
        .withCountryCode("GB")
        .execute()
        .getUrl
    }

  /**
   *
   * @return true if either the bank details are correct, or the rate limit for this enpoint is reached.
   *         In the latter case an error is logged.
   */
  def checkBankDetails(paymentData: DirectDebitData): Future[Boolean] = {
    Future {
      client.bankDetailsLookups().create()
        .withAccountNumber(paymentData.account)
        .withBranchCode(paymentData.sortCode)
        .withCountryCode("GB")
        .execute()
    } map { bdl =>
      bdl.getAvailableDebitSchemes.contains(AvailableDebitScheme.BACS)
    } recover { case e: GoCardlessApiException =>
      if (e.getCode == 429) {
        logger.error("Bypassing preliminary bank account check because the GoCardless rate limit has been reached for this endpoint. Someone might be using our website to proxy to GoCardless")
        true
      } else {
        false
      }
    }
  }
}
