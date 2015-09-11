package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.soap.models.Result.SubscribeResult
import com.gu.membership.zuora.{soap, ZuoraApiConfig}
import com.gu.membership.zuora.soap._
import com.gu.membership.zuora.soap.models.Query._
import com.gu.membership.zuora.soap.Readers._
import com.gu.membership.zuora.soap.actions.subscribe
import com.gu.membership.zuora.soap.actions.subscribe.Subscribe
import com.gu.monitoring.ServiceMetrics
import configuration.Config
import model.zuora.{DigitalProductPlan, SubscriptionProduct}
import model.{SubscriptionData, SubscriptionRequestData}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import touchpoint.ZuoraProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


trait ZuoraService {
  def subscriptionByName(id: String): Future[Subscription]
  def createSubscription(memberId: MemberId, data: SubscriptionData, requestData: SubscriptionRequestData): Future[SubscribeResult]
  def products: Future[Seq[SubscriptionProduct]]
  def ratePlans(subscription: Subscription): Future[Seq[RatePlan]]
  def defaultPaymentMethod(account: Account): Future[PaymentMethod]
  def account(subscription: Subscription): Future[Account]
  def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge]
}

class ZuoraApiClient(zuoraApiConfig: ZuoraApiConfig,
                     digitalProductPlan: DigitalProductPlan,
                     zuoraProperties: ZuoraProperties) extends ZuoraService {

  private val akkaSystem = Akka.system
  private val client = new soap.Client(zuoraApiConfig, new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client"), akkaSystem)
  private val cache: ProductsCache = new ProductsCache(client, akkaSystem, digitalProductPlan).refreshEvery(2.hour)

  def products = cache.items

  override def subscriptionByName(id: String): Future[Subscription] =
    client.queryOne[Subscription](SimpleFilter("Name", id))

  override def ratePlans(subscription: Subscription): Future[Seq[RatePlan]] =
    client.query[RatePlan](SimpleFilter("SubscriptionId", subscription.id))

  private def normalRatePlanCharges(ratePlan: RatePlan): Future[Seq[RatePlanCharge]] = {
    client.query[RatePlanCharge](AndFilter(
      "RatePlanId" -> ratePlan.id,
      "ChargeModel" -> "Flat Fee Pricing",
      "ChargeType" -> "Recurring"
    ))
  }

  override def defaultPaymentMethod(account: Account): Future[PaymentMethod] = {
    val paymentMethodId = account.defaultPaymentMethodId.getOrElse {
      throw new ZuoraServiceError(s"Could not find an account default payment method for $account")
    }
    client.queryOne[PaymentMethod](SimpleFilter("Id", paymentMethodId))
  }

  override def account(subscription: Subscription): Future[Account] =
    client.queryOne[Account](SimpleFilter("Id", subscription.accountId))

  override def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge] =
    for {
      plans <- ratePlans(subscription)
      charges <- Future.sequence { plans.map(normalRatePlanCharges) }.map(_.flatten)
    } yield charges.headOption.getOrElse {
      throw new ZuoraServiceError(s"Cannot find default subscription rate plan charge for $subscription")
  }

  override def createSubscription(memberId: MemberId, data: SubscriptionData, requestData: SubscriptionRequestData): Future[SubscribeResult] = {
    val account = subscribe.Account.stripe(memberId, autopay = true)

    val paymentMethod  = subscribe.BankTransfer(data.paymentData.holder,
                                                data.paymentData.account,
                                                data.paymentData.sortCode,
                                                data.personalData.firstName,
                                                data.personalData.lastName)

    client.authenticatedRequest(Subscribe(account = account,
                                paymentMethodOpt = Some(paymentMethod),
                                ratePlanId = data.ratePlanId,
                                firstName = data.personalData.firstName,
                                lastName = data.personalData.lastName,
                                address = data.personalData.address,
                                paymentDelay = Some(zuoraProperties.paymentDelayInDays),
                                casIdOpt = None,
                                ipAddressOpt = Some(requestData.ipAddress),
                                featureIds = Nil))
  }

}
