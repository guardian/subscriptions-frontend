package services

import configuration.Config
import model.PaymentData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GoCardlessService {
  def mandatePDFUrl(paymentData: PaymentData): Future[String]
}

object GoCardlessService extends GoCardlessService {
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
}
