package services

import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.github.nscala_time.time.Imports._
import com.gu.aws.CredentialsProvider
import com.gu.lib.Retry
import com.gu.memsub.Subscription._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{GetSalesforceContactForSub, PaymentService => CommonPaymentService}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan => Plan}
import com.gu.memsub.{Subscription => _, _}
import com.gu.monitoring.SafeLogger
import com.gu.salesforce.Contact
import com.gu.zuora.rest.ZuoraRestService
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import com.gu.monitoring.SafeLogger._
import configuration.Config
import logging.{Context, ContextLogging}
import model.SubscriptionOps._
import model.exactTarget.HolidaySuspensionBillingScheduleDataExtensionRow.constructSalutation
import model.exactTarget._
import model.{PurchaserIdentifiers, Renewal, SubscribeRequest}
import org.joda.time.{Days, LocalDate}
import play.api.libs.json._
import views.support.PlanOps._
import views.support.Pricing._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._
import scalaz.{-\/, EitherT, \/, \/-}
/**
  * Sends welcome email message to Amazon SQS queue which is consumed by membership-workflow.
  *
  * Exact Target expects the message to be in the following format:
  * https://code.exacttarget.com/apis-sdks/rest-api/v1/messaging/messageDefinitionSends.html
  */
class ExactTargetService(
  subscriptionService: subsv2.services.SubscriptionService[Future],
  paymentService: Future[CommonPaymentService],
  zuoraService: ZuoraService,
  salesforceService: SalesforceService
)(implicit val executionContext: ExecutionContext) extends ContextLogging {

  private def buildWelcomeEmailDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[DataExtensionRow] = {

    val zuoraPaidSubscription: Future[Subscription[Plan.Paid]] =
      Retry(2, scrub"Failed to get Zuora paid subscription ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        subscriptionData.productData.fold(
          { paper => subscriptionService.get[Plan.PaperPlan](Name(subscribeResult.subscriptionName)).map(_.get) },
          { digipack => subscriptionService.get[Plan.Digipack](Name(subscribeResult.subscriptionName)).map(_.get) })}

    val zuoraPaymentMethod: Future[PaymentMethod] =
      Retry(2, scrub"Failed to get Zuora payment method ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}") {
        paymentService.flatMap(_.getPaymentMethod(AccountId(subscribeResult.accountId)).map(
          _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}"))))}

    def buildRow(sub: Subscription[Plan.Paid], pm: PaymentMethod) = {
      val personalData = subscriptionData.genericData.personalData
      val promotionDescription = validPromotion.filterNot(_.promotion.promotionType == Tracking).map(_.promotion.description)

      val subscriptionDetails = {
        val currency = subscriptionData.genericData.currency

        val discountedPlanDescription = (for {
          vp <- validPromotion
          discountPromotion <- vp.promotion.asDiscount
        } yield {
          sub.plan.charges.prettyPricingForDiscountedPeriod(discountPromotion, currency)
        })

        def introductoryPeriodSubDescription = sub.introductoryPeriodPlan.map { introductoryPlan =>

          val nextRecurrringPeriod = sub.recurringPlans.minBy(_.start)

          introductoryPlan.charges.prettyPricing(currency) + " then " + nextRecurrringPeriod.charges.prettyPricing(currency)
        }

        discountedPlanDescription orElse introductoryPeriodSubDescription getOrElse sub.plan.charges.prettyPricing(currency)
      }


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
        case Failure(e) => SafeLogger.error(scrub"Failed to enqueue ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.", e)
      }
    }

  def enqueueETHolidaySuspensionEmail(
      subscription: Subscription[Plan.Delivery],
      packageName: String,
      billingSchedule: BillingSchedule,
      numberOfSuspensionsLinedUp: Int,
      daysUsed: Int,
      daysAllowed: Int
  )(implicit zuoraRestService: ZuoraRestService[Future]): Future[Unit] = {

    val buildDataExtensionRow =
      EitherT(GetSalesforceContactForSub(subscription)(zuoraService, salesforceService.repo, executionContext).flatMap { salesforceContact =>
        EitherT(zuoraRestService.getAccount(subscription.accountId)).map { account =>
          HolidaySuspensionBillingScheduleDataExtensionRow(
            email = account.billToContact.email,
            saluation = constructSalutation(salesforceContact.title, salesforceContact.firstName, Some(salesforceContact.lastName)),
            subscriptionName = subscription.name.get,
            subscriptionCurrency = subscription.currency,
            packageName = packageName,
            billingSchedule = billingSchedule,
            numberOfSuspensionsLinedUp = numberOfSuspensionsLinedUp,
            daysUsed = daysUsed,
            daysAllowed = daysAllowed
          )
        }.run
      })

    buildDataExtensionRow.flatMap { row =>
      EitherT(SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row).map {
        case Success(sendMsgResult) => \/.right(())
        case Failure(e) => \/.left(s"Details were: ${row.subscriptionName}, ${e.toString}")
      })
    }.run.map {
      case \/-(()) => logger.info(s"Successfully enqueued ${subscription.name.get}'s updated billing schedule email.")
      case -\/(e) => SafeLogger.error(scrub"Failed to enqueue ${subscription.name.get}'s updated billing schedule email. $e")
    }
  }

  def enqueueRenewalEmail(
    oldSub: Subscription[Plan.WeeklyPlan],
    renewal: Renewal,
    subscriptionDetails: String,
    contact: Contact,
    newTermStartDate: LocalDate)(implicit context: Context): Future[Unit] = {
    sealed trait Error {
      def msg: String
      def code: String
      def fullDescription = s"$code: $msg"
    }
    case class ExceptionThrown(msg: String, exception: Throwable) extends Error{
      val code = "ET001"
    }
    case class SimpleError(msg: String) extends Error{
      val code = "ET002"
    }

    def getPaymentMethod(accountId: AccountId): Future[\/[Error, PaymentMethod]] =
      paymentService.flatMap(_.getPaymentMethod(oldSub.accountId).map { maybePaymentMethod =>
        maybePaymentMethod.toRightDisjunction(SimpleError(s"Failed to enqueue guardian weekly renewal email. No payment method found in account"))
      })

    def sendToQueue(row: GuardianWeeklyRenewalDataExtensionRow): Future[\/[Error, SendMessageResult]] =
      SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row).map {
        case Success(sendMsgResult) => \/-(sendMsgResult)
        case Failure(e) => -\/(ExceptionThrown(s"Failed to enqueue ${oldSub.name.get}'s guardian weekly renewal email. Details were: " + row.toString, e))
      }

    val sentMessage = (for {
      paymentMethod <- EitherT(getPaymentMethod(oldSub.accountId))
      row = GuardianWeeklyRenewalDataExtensionRow(subscriptionName = oldSub.name,
        subscriptionDetails = subscriptionDetails,
        planName = renewal.plan.name,
        contact = contact,
        paymentMethod = paymentMethod,
        email = renewal.email,
        newTermStartDate = newTermStartDate)
      sendMessage <- EitherT(sendToQueue(row))
    } yield sendMessage).run

    sentMessage.map {
      case \/-(sendMsgResult) => info(s"Successfully enqueued guardian weekly renewal email.")
      case -\/(se@SimpleError(_)) => error(se.fullDescription)
      case -\/(et@ExceptionThrown(_, exception)) => error(et.fullDescription, exception)
    }.recover { case e: Exception => error(e.getMessage, e) }
  }
}

object SqsClient extends LazyLogging {
  private val sqsClient = AmazonSQSClient.builder
    .withCredentials(CredentialsProvider)
    .withRegion(EU_WEST_1)
    .build()

  def sendDataExtensionToQueue(queueName: String, row: DataExtensionRow)(implicit executionContext: ExecutionContext): Future[Try[SendMessageResult]] = {
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
