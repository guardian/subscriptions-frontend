package services

import com.gu.membership.zuora.soap.Readers._
import com.gu.membership.zuora.soap._
import com.gu.membership.zuora.soap.actions.subscribe.Subscribe
import com.gu.membership.zuora.soap.models.Queries._
import com.gu.membership.zuora.soap.models.Results.SubscribeResult
import com.gu.membership.zuora.{ZuoraSoapConfig, soap}
import com.gu.monitoring.ServiceMetrics
import configuration.Config
import model.zuora.{DigitalProductPlan, SubscriptionProduct}
import monitoring.TouchpointBackendMetrics
import org.joda.time.Period
import play.api.Play.current
import play.api.libs.concurrent.Akka
import touchpoint.ZuoraProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


trait ZuoraService {
  def subscriptionByName(id: String): Future[Subscription]
  def products: Future[Seq[SubscriptionProduct]]
  def ratePlans(subscription: Subscription): Future[Seq[RatePlan]]
  def defaultPaymentMethod(account: Account): Future[PaymentMethod]
  def account(subscription: Subscription): Future[Account]
  def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge]
  def subscribe(subscribe: Subscribe): Future[SubscribeResult]
  def paymentDelaysInDays: Period
}

class ZuoraApiClient(zuoraSoapConfig: ZuoraSoapConfig,
                     digitalProductPlan: DigitalProductPlan,
                     zuoraProperties: ZuoraProperties) extends ZuoraService {

  private val akkaSystem = Akka.system
  private val client = new soap.Client(zuoraSoapConfig, new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client"), akkaSystem)
  private val cache: ProductsCache = new ProductsCache(client, akkaSystem, digitalProductPlan, new TouchpointBackendMetrics {
    val backendEnv = zuoraSoapConfig.envName
    val service = "ProductsCache"
  }).refreshEvery(2.hour)

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

  override def subscribe(subscribe: Subscribe) = client.authenticatedRequest(subscribe)

  override def paymentDelaysInDays = zuoraProperties.paymentDelayInDays
}
