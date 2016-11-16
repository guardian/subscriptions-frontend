package services

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.gu.i18n.Currency
import com.gu.lib.Retry
import com.gu.memsub.Subscription._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{PaymentService => CommonPaymentService, GetSalesforceContactForSub}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan => Plan}
import com.gu.memsub.{Subscription => _, _}
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.exactTarget.HolidaySuspensionBillingScheduleDataExtensionRow.constructSalutation
import model.exactTarget._
import model.{PurchaserIdentifiers, SubscribeRequest}
import org.joda.time.Days
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import views.support.PlanOps._
import views.support.Pricing._

import scala.concurrent.Future
import scala.reflect.internal.util.StringOps
import scala.util.{Failure, Success, Try}
import scalaz.syntax.std.option._

/**
  * Sends welcome email message to Amazon SQS queue which is consumed by membership-workflow.
  *
  * Exact Target expects the message to be in the following format:
  * https://code.exacttarget.com/apis-sdks/rest-api/v1/messaging/messageDefinitionSends.html
  */
class ExactTargetService(
  subscriptionService: subsv2.services.SubscriptionService[Future],
  paymentService: CommonPaymentService,
  zuoraService: ZuoraService,
  salesforceService: SalesforceService
) extends LazyLogging {
  private def getPlanDescription(validPromotion: Option[ValidPromotion[NewUsers]], currency: Currency, plan: Plan.Paid): String = {
    (for {
      vp <- validPromotion
      discountPromotion <- vp.promotion.asDiscount
    } yield {
      plan.charges.prettyPricingForDiscountedPeriod(discountPromotion, currency)
    }).getOrElse(plan.charges.prettyPricing(currency))
  }

  private def buildWelcomeEmailDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[DataExtensionRow] = {

    val zuoraPaidSubscription: Future[Subscription[Plan.Paid]] =
      Retry(2, s"Failed to get Zuora paid subscription ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        subscriptionData.productData.fold(
          { paper => subscriptionService.get[Plan.PaperPlan](Name(subscribeResult.subscriptionName)).map(_.get) },
          { digipack => subscriptionService.get[Plan.Digipack](Name(subscribeResult.subscriptionName)).map(_.get) })}

    val zuoraPaymentMethod: Future[PaymentMethod] =
      Retry(2, s"Failed to get Zuora payment method ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        paymentService.getPaymentMethod(AccountId(subscribeResult.accountId)).map(
          _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}")))}

    def buildRow(sub: Subscription[Plan.Paid], pm: PaymentMethod) = {
      val personalData = subscriptionData.genericData.personalData
      val promotionDescription = validPromotion.filterNot(_.promotion.promotionType == Tracking).map(_.promotion.description)
      val subscriptionDetails = getPlanDescription(validPromotion, subscriptionData.genericData.currency, sub.plan)

      subscriptionData.productData.fold(
        paperData => if (paperData.plan.isHomeDelivery) {
          PaperHomeDeliveryWelcome1DataExtensionRow(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        } else if (paperData.plan.isGuardianWeekly) {
          GuardianWeeklyWelcome1DataExtensionRow(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        }
        else {
          PaperVoucherWelcome1DataExtensionRow(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        },
        _ => DigipackWelcome1DataExtensionRow(
          personalData = personalData,
          subscription = sub,
          paymentMethod = pm,
          gracePeriod = gracePeriod,
          subscriptionDetails = subscriptionDetails,
          promotionDescription = promotionDescription,
          currency = subscriptionData.genericData.currency
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

  def enqueueETWelcomeEmail(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[Unit] =

    for {
      row <- buildWelcomeEmailDataExtensionRow(subscribeResult, subscriptionData, gracePeriod, validPromotion, purchaserIds)
      response <- SqsClient.sendDataExtensionToQueue(Config.welcomeEmailQueue, row)
    } yield {
      response match {
        case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.")
        case Failure(e) => logger.error(s"Failed to enqueue ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.", e)
      }
    }

  def enqueueETHolidaySuspensionEmail(
      subscription: Subscription[Plan.Delivery],
      packageName: String,
      billingSchedule: BillingSchedule,
      numberOfSuspensionsLinedUp: Int,
      daysUsed: Int,
      daysAllowed: Int): Future[Unit] = {

    val buildDataExtensionRow =
      GetSalesforceContactForSub(subscription)(zuoraService, salesforceService.repo, defaultContext).map { salesforceContact =>
        salesforceContact.email.toRightDisjunction(s"no email in salesforce for ${subscription.id.get}").map { email =>
          HolidaySuspensionBillingScheduleDataExtensionRow(
            email = StringOps.oempty(email).headOption,
            saltuation = constructSalutation(salesforceContact.title, salesforceContact.firstName, Some(salesforceContact.lastName)),
            subscriptionName = subscription.name.get,
            subscriptionCurrency = subscription.plan.charges.price.currencies.head,
            packageName = packageName,
            billingSchedule = billingSchedule,
            numberOfSuspensionsLinedUp,
            daysUsed,
            daysAllowed
          )
        }
      }

    buildDataExtensionRow.flatMap { maybeRow =>
      val maybeFutureQueued = maybeRow.map { row =>
        SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row).map {
          case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscription.name.get}'s updated billing schedule email.")
          case Failure(e) => logger.error(s"Failed to enqueue ${subscription.name.get}'s updated billing schedule email. Details were: " + row.toString, e)
        }
      }
      maybeFutureQueued.fold({error =>
        logger.info(s"couldn't queue email: $error")
        Future.successful(())
      }, unit => unit)
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

      // FIXME the sendToQueue method is blocking, use an async way if there is one
      def sendToQueue(msg: String): SendMessageResult = {
        val queueUrl = sqsClient.createQueue(new CreateQueueRequest(queueName)).getQueueUrl
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msg))
      }

      Try(sendToQueue(payload))
    }
  }
}
