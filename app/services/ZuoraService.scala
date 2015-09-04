package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.{ZuoraApi, SimpleFilter, AndFilter, ZuoraServiceError}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.zuora.{DigitalProductPlan, SubscriptionProduct}
import model.{SubscriptionData, SubscriptionRequestData}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import services.zuora.Subscribe
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
  private val client = new ZuoraApi(zuoraApiConfig, new ZuoraMetrics(Config.stage, Config.appName), akkaSystem)
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

  override def createSubscription(memberId: MemberId, data: SubscriptionData, requestData: SubscriptionRequestData): Future[SubscribeResult] =
    client.authenticatedRequest(Subscribe(memberId, data, Some(zuoraProperties.paymentDelayInDays), requestData))
}
