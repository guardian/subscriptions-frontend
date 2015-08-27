package services

import com.gocardless.errors.GoCardlessApiException
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PaymentData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GoCardlessService {
  def mandatePDFUrl(paymentData: PaymentData): Future[String]
  def checkBankDetails(paymentData: PaymentData): Future[Boolean]
}

object GoCardlessService extends GoCardlessService with LazyLogging {
  lazy val client = Config.GoCardless.client

  override def mandatePDFUrl(paymentData: PaymentData): Future[String] =
    Future {
      client.mandatePdfs().create()
        .withAccountHolderName(paymentData.holder)
        .withAccountNumber(paymentData.account)
        .withBranchCode(paymentData.sortCode)
        .withCountryCode("GB")
        .execute()
        .getUrl
    }

  override def checkBankDetails(paymentData: PaymentData): Future[Boolean] = {
    val sortCode = paymentData.sortCode.replaceAllLiterally("-", "")

    Future {
      client.bankDetailsLookups().create()
        .withAccountNumber(paymentData.account)
        .withBranchCode(sortCode)
        .withCountryCode("GB")
        .execute()
      true
    } recover { case e: GoCardlessApiException =>
      false
    }
  }
}
