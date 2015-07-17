package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.{Login, ZuoraApi, ZuoraServiceError}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.SubscriptionData
import model.zuora.{BillingFrequency, SubscriptionProduct, DigitalProductPlan}
import org.joda.time.Period
import services.zuora.Subscribe
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait ZuoraService {
  def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult]
  def authTask: ScheduledTask[Authentication]
  def products: Seq[SubscriptionProduct]
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
    Config.Zuora.productsTaskIntervalSeconds.seconds)(getProducts())

  def products = productsTask.get()

  private def getProducts(): Future[Seq[SubscriptionProduct]] = {

    def productRatePlans: Future[Seq[ProductRatePlan]] =
      query[ProductRatePlan](s"ProductId='${digitalProductPlan.id}'")


    def productRatePlanCharges(productRatePlans: Seq[ProductRatePlan]): Future[Seq[ProductRatePlanCharge]] = {
      val queryString = productRatePlans.map(p => s"ProductRatePlanId='${p.id}'").mkString(" OR ")
      query[ProductRatePlanCharge](queryString)
    }

    def productRatePlanChargeTiers(productRatePlanCharges: Seq[ProductRatePlanCharge]): Future[Seq[ProductRatePlanChargeTier]] = {
      val queryString = productRatePlanCharges.map(p => s"ProductRatePlanChargeId='${p.id}'").mkString(" OR ")
      query[ProductRatePlanChargeTier](queryString)
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
