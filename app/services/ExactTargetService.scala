package services

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.gu.i18n.Currency
import com.gu.memsub.Subscription._
import com.gu.memsub._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{SubscriptionService, PaymentService => CommonPaymentService}
import com.gu.subscriptions.{DigipackCatalog, PaperCatalog}
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.exactTarget.HolidaySuspensionBillingScheduleDataExtensionRow.constructSalutation
import model.{PurchaserIdentifiers, SubscribeRequest}
import model.exactTarget._
import org.joda.time.Days
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import views.support.Pricing._

import scala.concurrent.Future
import scala.reflect.internal.util.StringOps
import scala.util.{Failure, Success, Try}
import utils.Retry

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
  def zuoraService: ZuoraService
  def salesforceService: SalesforceService

  private def getPlanDescription(validPromotion: Option[ValidPromotion[NewUsers]], currency: Currency, plan: PaidPlan[Status, BillingPeriod]): String = {
    (for {
      vp <- validPromotion
      discountPromotion <- vp.promotion.asDiscount
    } yield {
      plan.prettyPricingForDiscountedPeriod(discountPromotion, currency)
    }).getOrElse(plan.prettyPricing(currency))
  }

  private def buildETDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[DataExtensionRow] = {

    val zuoraPaidSubscription: Future[PaidSub] =
      Retry(2, s"Failed to get Zuora paid subscription ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        subscriptionData.productData.fold(
          { paper => paperSubscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName)) },
          { digipack => digiSubscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName)) })}

    val zuoraPaymentMethod: Future[PaymentMethod] =
      Retry(2, s"Failed to get Zuora payment method ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        paymentService.getPaymentMethod(Subscription.AccountId(subscribeResult.accountId)).map(
          _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}")))}

    def buildRow(sub: PaidSub, pm: PaymentMethod) = {
      val personalData = subscriptionData.genericData.personalData
      val promotionDescription = validPromotion.filterNot(_.promotion.promotionType == Tracking).map(_.promotion.description)
      val subscriptionDetails = getPlanDescription(validPromotion, personalData.currency, sub.plan)

      subscriptionData.productData.fold(
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
    }

    for {
      sub <- zuoraPaidSubscription
      pm <- zuoraPaymentMethod
    } yield {
      buildRow(sub, pm)
    }
  }

  def sendETDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[Unit] =

    for {
      row <- buildETDataExtensionRow(subscribeResult, subscriptionData, gracePeriod, validPromotion, purchaserIds)
      response <- SqsClient.sendDataExtensionToQueue(Config.welcomeEmailQueue, row)
    } yield {
      response match {
        case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.")
        case Failure(e) => logger.error(s"Failed to enqueue ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.", e)
      }
    }

  def sendETDataExtensionRow(
      subscription: Subscription,
      packageName: String,
      billingSchedule: BillingSchedule,
      numberOfSuspensionsLinedUp: Int,
      daysUsed: Int,
      daysAllowed: Int): Future[Unit] = {

    for {
      zuoraAccount <- zuoraService.getAccount(subscription.accountId)
      sfContactId <- zuoraAccount.sfContactId.fold[Future[String]](Future.failed(new IllegalStateException(s"Zuora record for ${subscription.accountId} has no sfContactId")))(Future.successful)
      salesforceContact <- salesforceService.repo.get(sfContactId).map(_.getOrElse(throw new IllegalStateException(s"Cannot find salesforce contact for $sfContactId")))
      row = HolidaySuspensionBillingScheduleDataExtensionRow(
        email = StringOps.oempty(salesforceContact.email).headOption,
        saltuation = constructSalutation(salesforceContact.title, salesforceContact.firstName, Some(salesforceContact.lastName)),
        subscriptionName = subscription.name.get,
        subscriptionCurrency = subscription.currency,
        packageName = packageName,
        billingSchedule = billingSchedule,
        numberOfSuspensionsLinedUp,
        daysUsed,
        daysAllowed
      )
      response <- SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row)
    } yield {
      response match {
        case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscription.name.get}'s updated billing schedule email.")
        case Failure(e) => logger.error(s"Failed to enqueue ${subscription.name.get}'s updated billing schedule email. Details were: " + row.toString, e)
      }
    }
  }
}

object SqsClient extends LazyLogging {
  private val sqsClient = new AmazonSQSClient()
  sqsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def sendDataExtensionToQueue(queueName: String, row: DataExtensionRow): Future[Try[SendMessageResult]] = {
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
        val queueUrl = sqsClient.createQueue(new CreateQueueRequest(queueName)).getQueueUrl
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msg))
      }

      Try(sendToQueue(payload))
    }
  }
}
