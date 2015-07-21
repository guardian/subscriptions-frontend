package services

import java.net.URL

import configuration.Config
import model.PaymentData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GoCardlessService {
  def mandatePDF(paymentData: PaymentData): Future[URL]
}

object GoCardlessService extends GoCardlessService {
  lazy val client = Config.GoCardless.client

  override def mandatePDF(paymentData: PaymentData): Future[URL] =
    Future {
      val pdf = client.mandatePdfs().create()
        .withAccountHolderName(paymentData.holder)
        .withAccountNumber(paymentData.account)
        .withBranchCode(paymentData.sortCode)
        .withCountryCode("GB")
        .execute()
      new URL(pdf.getUrl)
    }
}
