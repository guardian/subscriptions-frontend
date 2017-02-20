package services

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.gu.aws.CredentialsProvider
import com.gu.i18n.Currency
import com.gu.lib.Retry
import com.gu.memsub.Subscription._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{GetSalesforceContactForSub, PaymentService => CommonPaymentService}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan => Plan}
import com.gu.memsub.{Subscription => _, _}
import com.gu.salesforce.Contact
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.exactTarget.HolidaySuspensionBillingScheduleDataExtensionRow.constructSalutation
import model.exactTarget._
import model.{PaperData, PurchaserIdentifiers, Renewal, SubscribeRequest, SubscriptionOps}
import model.SubscriptionOps._
import org.joda.time.{DateTime, Days, LocalDate}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import views.support.PlanOps._
import views.support.Pricing._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.{-\/, EitherT, \/, \/-}
import scalaz.Scalaz._

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

      //todo just for test see how to fix this
      val now = DateTime.now.toLocalDate

      val nonExpiredPlans = sub.plans.list.filter(_.end.isAfter(now))
      val plan = nonExpiredPlans.head //todo this should probably continue using the whole plan instead of passing just one

      //val subscriptionDetails = getPlanDescription(validPromotion, subscriptionData.genericData.currency, plan)
      val subscriptionDetails = sub.newSubPaymentDescripttion(validPromotion, subscriptionData.genericData.currency)
      subscriptionData.productData.fold(
        paperData =>{
          println(s"paper date is $paperData")
          if (paperData.plan.isHomeDelivery) {
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
        }
    }
        ,
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
        HolidaySuspensionBillingScheduleDataExtensionRow(
          email = salesforceContact.email,
          saluation = constructSalutation(salesforceContact.title, salesforceContact.firstName, Some(salesforceContact.lastName)),
          subscriptionName = subscription.name.get,
          subscriptionCurrency = subscription.currency,
          packageName = packageName,
          billingSchedule = billingSchedule,
          numberOfSuspensionsLinedUp = numberOfSuspensionsLinedUp,
          daysUsed = daysUsed,
          daysAllowed = daysAllowed
        )
      }

    buildDataExtensionRow.flatMap { row =>
      SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row).map {
        case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscription.name.get}'s updated billing schedule email.")
        case Failure(e) => logger.error(s"Failed to enqueue ${subscription.name.get}'s updated billing schedule email. Details were: " + row.toString, e)
      }
    }
  }

  def enqueueRenewalEmail(
    oldSub: Subscription[Plan.WeeklyPlan],
    renewal: Renewal,
    subscriptionDetails: String,
    contact: Contact,
    email: String,
    customerAcceptance: LocalDate,
    contractEffective: LocalDate): Future[Unit] = {

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

    def getPaymentMethod(accountId: AccountId): Future[\/[Error, PaymentMethod]] = paymentService.getPaymentMethod(oldSub.accountId).map(_.toRightDisjunction(SimpleError(s"Failed to enqueue ${oldSub.name.get}'s guardian weekly renewal email. No payment method found in account")))

    def sendToQueue(row: GuardianWeeklyRenewalDataExtensionRow): Future[\/[Error, SendMessageResult]] = SqsClient.sendDataExtensionToQueue(Config.holidaySuspensionEmailQueue, row).map {
      case Success(sendMsgResult) => \/-(sendMsgResult)
      case Failure(e) => -\/(ExceptionThrown(s"Failed to enqueue ${oldSub.name.get}'s guardian weekly renewal email. Details were: " + row.toString, e))
    }

    val sentMessage = (for {
      paymentMethod <- EitherT(getPaymentMethod(oldSub.accountId))
      row = GuardianWeeklyRenewalDataExtensionRow(oldSub.name,
        renewal.plan.name,
        subscriptionDetails,
        contact,
        paymentMethod,
        email,
        customerAcceptance,
        contractEffective)
      sendMessage <- EitherT(sendToQueue(row))
    } yield (sendMessage)).run

    sentMessage.map {
      case \/-(sendMsgResult) => logger.info(s"Successfully enqueued ${oldSub.name.get}'s guardian weekly renewal email.")
      case -\/(se@SimpleError(_)) => logger.error(se.fullDescription)
      case -\/(et@ExceptionThrown(_,exception)) => logger.error(et.fullDescription, exception)
    }
  }
}

object SqsClient extends LazyLogging {
  private val sqsClient = new AmazonSQSClient(CredentialsProvider)
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
