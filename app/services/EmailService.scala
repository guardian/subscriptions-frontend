package services

import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.github.nscala_time.time.Imports._
import com.gu.aws.CredentialsProvider
import com.gu.memsub.Subscription._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{SalesforceContactServiceUsingZuoraRest, PaymentService => CommonPaymentService}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan => Plan}
import com.gu.memsub.{Subscription => _, _}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.salesforce.{Contact, SFContactId}
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.rest.ZuoraRestService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import logging.{Context, ContextLogging}
import model.SubscriptionOps._
import model.HolidaySuspensionBillingScheduleEmail.constructSalutation
import model._
import model.{PurchaserIdentifiers, Renewal, SubscribeRequest}
import org.joda.time.{Days, LocalDate}
import play.api.libs.json._
import scalaz.Scalaz._
import scalaz.{-\/, EitherT, \/, \/-}
import views.support.PlanOps._
import views.support.Pricing._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Publishes messages to membership-workflow SQS queues
  * https://github.com/guardian/membership-workflow
  */
class EmailService(
  subscriptionService: subsv2.services.SubscriptionService[Future],
  paymentService: Future[CommonPaymentService],
  zuoraService: ZuoraService,
  salesforceService: SalesforceService
)(implicit val executionContext: ExecutionContext) extends ContextLogging {

  private def buildEmailSqsMessage(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[Email] = {

    val zuoraPaidSubscription: Future[Subscription[Plan.Paid]] =
      subscriptionData.productData.fold(
        { paper => subscriptionService.get[Plan.PaperPlan](Name(subscribeResult.subscriptionName)).map(_.get) },
        { digipack => subscriptionService.get[Plan.Digipack](Name(subscribeResult.subscriptionName)).map(_.get) })
    zuoraPaidSubscription.onFailure { case t =>
      SafeLogger.error(scrub"Failed to get Zuora paid subscription ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}", t)
    }

    val zuoraPaymentMethod: Future[PaymentMethod] =
      paymentService.flatMap(_.getPaymentMethod(AccountId(subscribeResult.accountId)).map(
        _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}"))))
    zuoraPaymentMethod.onFailure { case t =>
      SafeLogger.error(scrub"Failed to get Zuora payment method ${subscribeResult.subscriptionName} for ${purchaserIds.identityId}", t)
    }

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
          PaperHomeDeliveryWelcome1Email(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        } else if (paperData.plan.isGuardianWeekly) {
          GuardianWeeklyWelcome1Email(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        }
        else {
          PaperVoucherWelcome1Email(
            paperData = paperData,
            personalData = personalData,
            subscription = sub,
            paymentMethod = pm,
            subscriptionDetails = subscriptionDetails,
            promotionDescription = promotionDescription
          )
        },
        _ => DigipackWelcome1Email(
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

  def enqueueWelcomeEmail(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscribeRequest,
      gracePeriod: Days,
      validPromotion: Option[ValidPromotion[NewUsers]],
      purchaserIds: PurchaserIdentifiers): Future[Unit] =

    for {
      message <- buildEmailSqsMessage(subscribeResult, subscriptionData, gracePeriod, validPromotion, purchaserIds)
      response <- MembershipWorkflowSqsClient.sendMessage(Config.welcomeEmailQueue, message, SFContactId(purchaserIds.buyerContactId.salesforceContactId))
    } yield {
      response match {
        case Success(sendMsgResult) => logger.info(s"Successfully enqueued ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.")
        case Failure(e) => SafeLogger.error(scrub"Failed to enqueue ${subscribeResult.subscriptionName} welcome email for user ${purchaserIds.identityId}.", e)
      }
    }

  def enqueueHolidaySuspensionEmail(
      subscription: Subscription[Plan.Delivery],
      packageName: String,
      billingSchedule: BillingSchedule,
      numberOfSuspensionsLinedUp: Int,
      daysUsed: Int,
      daysAllowed: Int
  )(implicit zuoraRestService: ZuoraRestService[Future]): Future[Unit] = {

    def buildHolidaySuspensionEmailSqsMessage(salesforceContact: Contact, zuoraAccount: ZuoraRestService.AccountSummary) = {
      HolidaySuspensionBillingScheduleEmail(
        email = zuoraAccount.billToContact.email,
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

    def sendToQueue(salesforceContact: Contact, email: HolidaySuspensionBillingScheduleEmail) = {
      MembershipWorkflowSqsClient.sendMessage(Config.holidaySuspensionEmailQueue, email, SFContactId(salesforceContact.salesforceContactId)).map {
        case Success(_) => \/.right(())
        case Failure(e) => \/.left(s"Details were: ${email.subscriptionName}, ${e.toString}")
      }
    }

    val enqueueResult = for {
      zuoraAccount <- EitherT(zuoraRestService.getAccount(subscription.accountId))
      salesforceContact <- EitherT.right(SalesforceContactServiceUsingZuoraRest.getBuyerContactForZuoraAccount(zuoraAccount)(salesforceService.repo, executionContext))
      message = buildHolidaySuspensionEmailSqsMessage(salesforceContact, zuoraAccount)
      result <- EitherT(sendToQueue(salesforceContact, message))
    } yield result

    enqueueResult.run.map {
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

    def sendToQueue(email: GuardianWeeklyRenewalEmail): Future[\/[Error, SendMessageResult]] = {

      MembershipWorkflowSqsClient.sendMessage(Config.holidaySuspensionEmailQueue, email, SFContactId(contact.salesforceContactId)).map {
        case Success(sendMsgResult) => \/-(sendMsgResult)
        case Failure(e) => -\/(ExceptionThrown(s"Failed to enqueue ${oldSub.name.get}'s guardian weekly renewal email. Details were: " + email.toString, e))
      }
    }

    val sentMessage = (for {
      paymentMethod <- EitherT(getPaymentMethod(oldSub.accountId))
      email = GuardianWeeklyRenewalEmail(subscriptionName = oldSub.name,
        subscriptionDetails = subscriptionDetails,
        planName = renewal.plan.name,
        contact = contact,
        paymentMethod = paymentMethod,
        email = renewal.email,
        newTermStartDate = newTermStartDate)
      sendMessage <- EitherT(sendToQueue(email))
    } yield sendMessage).run

    sentMessage.map {
      case \/-(sendMsgResult) => info(s"Successfully enqueued guardian weekly renewal email.")
      case -\/(se@SimpleError(_)) => error(se.fullDescription)
      case -\/(et@ExceptionThrown(_, exception)) => error(et.fullDescription, exception)
    }.recover { case e: Exception => error(e.getMessage, e) }
  }
}

object MembershipWorkflowSqsClient extends LazyLogging {
  private val sqsClient = AmazonSQSClient.builder
    .withCredentials(CredentialsProvider)
    .withRegion(EU_WEST_1)
    .build()

  def sendMessage(queueName: String, emailMessage: Email, sfContactId: SFContactId)(implicit executionContext: ExecutionContext): Future[Try[SendMessageResult]] = {
    Future {
      val payload = Json.obj(
        "To" -> Json.obj(
          "Address" -> emailMessage.email,
          "SubscriberKey" -> emailMessage.email,
          "ContactAttributes" -> Json.obj(
            "SubscriberAttributes" ->  Json.toJsFieldJsValueWrapper(emailMessage.fields.toMap)
          )
        ),
        "DataExtensionName" -> emailMessage.forExtension.name,
        "SfContactId" -> sfContactId.get
      ).toString

      // FIXME the sendToQueue method is blocking, use an async way if there is one
      def sendToQueue(msg: String): SendMessageResult = {
        val queueUrl = sqsClient.createQueue(new CreateQueueRequest(queueName)).getQueueUrl
        sqsClient.sendMessage(new SendMessageRequest(queueUrl, msg))
      }

      Try(sendToQueue(payload))
    }
  }
}
