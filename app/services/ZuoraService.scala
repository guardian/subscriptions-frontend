package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.ZuoraReaders.ZuoraQueryReader
import com.gu.membership.zuora.soap.{Login, ZuoraApi, ZuoraServiceError}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.SubscriptionData
import model.zuora.{BillingFrequency, DigitalProductPlan, SubscriptionProduct}
import services.zuora.Subscribe
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

sealed trait ZuoraFilter {
  def toFilterString: String
}

case class SimpleFilter(key: String, value: String) extends ZuoraFilter {
  override def toFilterString = s"$key='$value'"
}

case class AndFilter(clauses: (String, String)*) extends ZuoraFilter {
  override def toFilterString =
    clauses.map(p => SimpleFilter.tupled(p).toFilterString).mkString(" AND ")
}

case class OrFilter(clauses: (String, String)*) extends ZuoraFilter {
  override def toFilterString =
    clauses.map(p => SimpleFilter.tupled(p).toFilterString).mkString(" OR ")
}

trait ZuoraService {
  def subscriptionByName(id: String): Future[Subscription]
  def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult]
  def authTask: ScheduledTask[Authentication]
  def products: Seq[SubscriptionProduct]
  def ratePlans(subscription: Subscription): Future[Seq[RatePlan]]
  def defaultPaymentMethod(account: Account): Future[PaymentMethod]
  def account(subscription: Subscription): Future[Account]
  def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge]
}

class ZuoraApiClient(zuoraApiConfig: ZuoraApiConfig, digitalProductPlan: DigitalProductPlan) extends ZuoraApi with ZuoraService {
  override implicit def authentication: Authentication = authTask.get()

  override val apiConfig = zuoraApiConfig
  override val application = Config.appName
  override val stage = Config.stage

  override val metrics = new ZuoraMetrics(stage, application)

  lazy val productsSchedule = productsTask.start()

  override val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes){
    val authF = request(Login(apiConfig))
    authF.filter(_.token.nonEmpty)foreach(_ => productsSchedule)
    authF
  }

  val productsTask = ScheduledTask[Seq[SubscriptionProduct]]("Loading rate plans", Nil, 0.seconds,
    Config.Zuora.productsTaskIntervalSeconds.seconds)(getProducts)

  private def query[T <: ZuoraQuery](where: ZuoraFilter)(implicit reader: ZuoraQueryReader[T]): Future[Seq[T]] =
    query(where.toFilterString)(reader)

  private def queryOne[T <: ZuoraQuery](where: ZuoraFilter)(implicit reader: ZuoraQueryReader[T]): Future[T] =
    queryOne(where.toFilterString)(reader)

  override def subscriptionByName(id: String): Future[Subscription] =
    queryOne[Subscription](SimpleFilter("Name", id))

  override def ratePlans(subscription: Subscription): Future[Seq[RatePlan]] =
    query[RatePlan](SimpleFilter("SubscriptionId", subscription.id))

  private def normalRatePlanCharges(ratePlan: RatePlan): Future[Seq[RatePlanCharge]] = {
    query[RatePlanCharge](AndFilter(
      "RatePlanId" -> ratePlan.id,
      "ChargeModel" -> "Flat Fee Pricing",
      "ChargeType" -> "Recurring"
    ))
  }

  override def defaultPaymentMethod(account: Account): Future[PaymentMethod] = {
    val paymentMethodId = account.defaultPaymentMethodId.getOrElse {
      throw new ZuoraServiceError(s"Could not find an account default payment method for $account")
    }
    queryOne[PaymentMethod](SimpleFilter("Id", paymentMethodId))
  }

  override def account(subscription: Subscription): Future[Account] =
    queryOne[Account](SimpleFilter("Id", subscription.accountId))

  override def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge] =
    for {
      pr <- ratePlans(subscription)
      rpcs <- Future.sequence { pr.map(normalRatePlanCharges) }.map(_.flatten)
    } yield rpcs.headOption.getOrElse {
      throw new ZuoraServiceError(s"Cannot find default subscription rate plan charge for $subscription")
    }


  def products = productsTask.get()

  private def getProducts: Future[Seq[SubscriptionProduct]] = {

    def productRatePlans: Future[Seq[ProductRatePlan]] =
      query[ProductRatePlan](SimpleFilter("ProductId", digitalProductPlan.id))

    def productRatePlanCharges(productRatePlans: Seq[ProductRatePlan]): Future[Seq[ProductRatePlanCharge]] = {
      val clauses = productRatePlans.map(p => "ProductRatePlanId" -> p.id)
      query[ProductRatePlanCharge](OrFilter(clauses: _*))
    }

    def productRatePlanChargeTiers(productRatePlanCharges: Seq[ProductRatePlanCharge]): Future[Seq[ProductRatePlanChargeTier]] = {
      val clauses = productRatePlanCharges.map(p => "ProductRatePlanChargeId" -> p.id)
      query[ProductRatePlanChargeTier](OrFilter(clauses: _*))
    }

    for {
      productRatePlan <- productRatePlans
      productRatePlanCharges <- productRatePlanCharges(productRatePlan)
      productRatePlanChargesTier <- productRatePlanChargeTiers(productRatePlanCharges)
    } yield {

      //filter by GBP rate plans
      val productRatePlanChargesTierPounds = productRatePlanChargesTier.filter(_.currency == "GBP")

      //filter by plans that have a billing frequency we have
      val validProductRatePlanCharges = productRatePlanCharges.filter(b => BillingFrequency.all.map(_.lowercase).contains(b.billingPeriod.toLowerCase))

      //filter product rate plans by ones that are in date
      val validRatePlans = productRatePlan.filter { p =>
        p.effectiveStartDate.isBeforeNow && p.effectiveEndDate.isAfterNow
      }

      //pull all the queries together to make a SubscriptionProduct

      val subscriptionProducts = for(ratePlan <- validRatePlans) yield {
        val billingFreqProductRatePlan = validProductRatePlanCharges.find(_.productRatePlanId == ratePlan.id)
        val productRatePlanChargeTier = billingFreqProductRatePlan.flatMap { p =>
          productRatePlanChargesTierPounds.find(_.productRatePlanChargeId == p.id)
        }

        val billingFreq = billingFreqProductRatePlan.flatMap(a => BillingFrequency.all.find(a.billingPeriod.toLowerCase == _.lowercase))

        if (billingFreq.isDefined && productRatePlanChargeTier.isDefined)
          SubscriptionProduct(digitalProductPlan, billingFreq.get, ratePlan.id, productRatePlanChargeTier.get.price)
        else throw new ZuoraServiceError(s"Could not find valid ratePlan details for Digital Pack. Rate plan ID requested ${ratePlan.id}")
      }
      subscriptionProducts
    }
  }

  override def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult] = {
    request(Subscribe(memberId, data))
  }
}
