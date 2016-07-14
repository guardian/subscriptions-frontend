package services

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import views.support.Pricing._
import com.gu.i18n.Currency
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{SubscriptionService, PaymentService => CommonPaymentService}
import com.gu.memsub._
import com.gu.subscriptions.{DigipackCatalog, PaperCatalog}
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.SubscribeRequest
import model.exactTarget._
import org.joda.time.Days
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Sends welcome email message to Amazon SQS queue which is consumed by membership-workflow.
  *
  * Exact Target expects the message to be in the following format:
  * https://code.exacttarget.com/apis-sdks/rest-api/v1/messaging/messageDefinitionSends.html
  */
trait ExactTargetService extends LazyLogging {
  def digiSubscriptionService: SubscriptionService[DigipackCatalog]
  def paperSubscriptionService: SubscriptionService[PaperCatalog]
  def paymentService: CommonPaymentService

  private def getPlanDescription(validPromotion: Option[ValidPromotion[NewUsers]], currency: Currency, plan: PaidPlan[Status, BillingPeriod]): String = {
    (for {
      vp <- validPromotion
      discountPromotion <- vp.promotion.asDiscount
    } yield {
      plan.prettyPricingForDiscountedPeriod(discountPromotion, currency)
    }).getOrElse(plan.prettyPricing(currency))
  }

  def sendETDataExtensionRow(
                              subscribeResult: SubscribeResult,
                              subscriptionData: SubscribeRequest,
                              gracePeriod: Days,
                              validPromotion: Option[ValidPromotion[NewUsers]]): Future[Unit] = {

    val subscription = subscriptionData.productData.fold({ paper =>
      paperSubscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName))
    }, {digipack =>
      digiSubscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName))
    })

    val paymentMethod = paymentService.getPaymentMethod(Subscription.AccountId(subscribeResult.accountId)).map(
      _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}"))
    )

    val promotionDescription = validPromotion.filterNot(_.promotion.promotionType == Tracking).map(_.promotion.description)

    val personalData = subscriptionData.genericData.personalData

    val subscriptionDetails =
      for {
        sub <- subscription
        subscriptionDetails = getPlanDescription(validPromotion, personalData.currency, sub.plan)
        pm <- paymentMethod
        row = subscriptionData.productData.fold(
          paperData => PaperHomeDeliveryWelcome1DataExtensionRow(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          ),
          _ => DigipackWelcome1DataExtensionRow(
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            gracePeriod = gracePeriod,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        )
        response <- SqsClient.sendWelcomeEmailToQueue(row)
      } yield {
        response match {
          case Success(sendMsgResult) => logger.info(s"Successfully sent ${subscribeResult.subscriptionName} welcome email.")
          case Failure(e) => logger.error(s"Failed to send ${subscribeResult.subscriptionName} welcome email.", e)
        }
      }
  }
}

object SqsClient extends LazyLogging {
  private val sqsClient = new AmazonSQSClient()
  sqsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def sendWelcomeEmailToQueue(row: DataExtensionRow): Future[Try[SendMessageResult]] = {
    Future {
      val payload = Json.obj(
        "To" -> Json.obj(
          "Address" -> row.email,
          "SubscriberKey" -> row.email,
          "ContactAttributes" -> Json.obj(
            "SubscriberAttributes" ->  Json.toJsFieldJsValueWrapper(row.fields.toMap)
          )
        ),
        "DataExtensionName" -> row.forExtension.name
      ).toString()

      def sendToQueue(msg: String): SendMessageResult = {
        val queueUrl = sqsClient.createQueue(new CreateQueueRequest(Config.welcomeEmailQueue)).getQueueUrl
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msg))
      }

      Try(sendToQueue(payload))
    }
  }
}
