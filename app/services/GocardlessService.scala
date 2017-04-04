package services

import com.gocardless.errors.GoCardlessApiException
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.DirectDebitData

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

trait GoCardlessService {
  def mandatePDFUrl(paymentData: DirectDebitData): Future[String]
  def checkBankDetails(paymentData: DirectDebitData): Future[Boolean]
}

object GoCardlessService extends GoCardlessService with LazyLogging {
  lazy val client = Config.GoCardless.client

  override def mandatePDFUrl(paymentData: DirectDebitData): Future[String] =
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
  override def checkBankDetails(paymentData: DirectDebitData): Future[Boolean] = {
    val sortCode = paymentData.sortCode.replaceAllLiterally("-", "")

    Future {
      client.bankDetailsLookups().create()
        .withAccountNumber(paymentData.account)
        .withBranchCode(sortCode)
        .withCountryCode("GB")
        .execute()
      true
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
