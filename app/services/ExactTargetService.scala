package services

import com.exacttarget.fuelsdk.{ETClient, ETDataExtensionRow, ETResult}
import com.gu.membership.zuora.soap.Zuora.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.SubscriptionData
import model.exactTarget.{DataExtensionRow, SubscriptionDataExtensionRow}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ExactTargetService extends LazyLogging {
  def etClient: ETClient
  def thankYouDataExtensionKey: String

  def makeETRow(row: DataExtensionRow) = {
    val etRow = new ETDataExtensionRow
    row.fields.foreach { case (k, v) =>
      etRow.setColumn(k.toString, v)
    }
    etRow.setDataExtensionKey(thankYouDataExtensionKey)
    etRow
  }

  def sendETDataExtensionRow(subscribeResult: SubscribeResult, subscriptionData: SubscriptionData, zs: ZuoraService): Future[Unit] = {
    val subscription = zs.subscriptionByName(subscribeResult.name)

    val  accAndPaymentMethod= for {
      subs <- subscription
      acc <- zs.account(subs)
      pm <- zs.defaultPaymentMethod(acc)
    } yield (acc, pm)

    for {
      subs <- subscription
      rpc <- zs.normalRatePlanCharge(subs)
      (acc, pm) <- accAndPaymentMethod
      row = SubscriptionDataExtensionRow(
        subscription = subs,
        subscriptionData = subscriptionData,
        ratePlanCharge = rpc,
        paymentMethod = pm,
        account = acc
      )
      etRow = makeETRow(row)
      response <- Future { etClient.create(etRow) }
    } yield {
      if (response.getStatus == ETResult.Status.OK)
        logger.info(s"Successfully sent an email to confirm the subscription: $subscribeResult")
      else
        logger.error(s"Failed to confirm the subscription $subscribeResult. Code: ${response.getResponseCode}, message: ${response.getResponseMessage}")
    }
  }
}

object ExactTargetService extends ExactTargetService {
  lazy val etClient = Config.ExactTarget.etClient
  lazy val thankYouDataExtensionKey = Config.ExactTarget.thankYouDataExtensionKey
}
