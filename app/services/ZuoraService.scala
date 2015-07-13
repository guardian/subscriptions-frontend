package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.{ZuoraServiceError, Login, ZuoraApi}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.SubscriptionData
import model.zuora.ProductPlan.Digital
import model.zuora.{BillingFrequency, SubscriptionProduct}
import org.joda.time.Period
import services.zuora.Subscribe
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ZuoraService(zuoraApiConfig: ZuoraApiConfig, digitalProductId: String) extends ZuoraApi {

  override val apiConfig: ZuoraApiConfig = zuoraApiConfig

  override implicit def authentication: Authentication = authTask.get()

  override val application: String = Config.appName
  override val stage: String = Config.stage

  override val metrics = new ZuoraMetrics(stage, application)
  val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(request(Login(apiConfig)))

  def createSubscription(memberId: MemberId, data: SubscriptionData, paymentDelay: Option[Period]): Future[SubscribeResult] = {
    request(Subscribe(memberId, data, paymentDelay))
  }

  private def getProducts(): Future[Seq[SubscriptionProduct]] = {

    def productRatePlans: Future[Seq[ProductRatePlan]] =
      query[ProductRatePlan](s"ProductId='$digitalProductId'")


    def productRatePlanCharges(productRatePlans: Seq[ProductRatePlan]): Future[Seq[ProductRatePlanCharge]] = {
      Future.traverse(productRatePlans) {  productRatePlan =>
          query[ProductRatePlanCharge](s"ProductRatePlanId='${productRatePlan.id}'")
        }.map(_.flatten)
      }


    def productRatePlanChargeTiers(productRatePlanCharges: Seq[ProductRatePlanCharge]): Future[Seq[ProductRatePlanChargeTier]] = {
      Future.traverse(productRatePlanCharges) { productRatePlanCharge =>
        query[ProductRatePlanChargeTier](s"ProductRatePlanChargeId='${productRatePlanCharge.id}'")
      }.map(_.flatten)

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
          SubscriptionProduct(Digital, billingFreq.get, ratePlan.id, productRatePlanChargeTier.get.price)
        else throw new ZuoraServiceError(s"Could not find valid ratePlan details for Digital Pack. Rate plan ID requested ${ratePlan.id}")
      }
      subscriptionProducts
    }
  }

  val productsTask = ScheduledTask[Seq[SubscriptionProduct]]("Loading rate plans", Nil, 1.seconds, 1.day)(getProducts())

  def products = productsTask.get()
}



