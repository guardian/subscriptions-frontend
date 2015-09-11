package services

import akka.actor.ActorSystem
import com.gu.membership.util.FutureSupplier
import com.gu.membership.zuora.soap
import com.gu.membership.zuora.soap._
import com.gu.membership.zuora.soap.models.Query._
import com.gu.membership.zuora.soap.Readers._
import model.zuora.{BillingFrequency, DigitalProductPlan, SubscriptionProduct}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

protected class ProductsCache(client: soap.Client, akkaSystem: ActorSystem, digitalProductPlan: DigitalProductPlan) {

  def items = productsSupplier.get()

  private val productsSupplier = new FutureSupplier[Seq[SubscriptionProduct]](getSubscriptions)

  def refreshEvery(refreshDuration : FiniteDuration) = {
    akkaSystem.scheduler.schedule(refreshDuration, refreshDuration) {
      productsSupplier.refresh()
    }
    this
  }

  private def productRatePlans: Future[Seq[ProductRatePlan]] =
    client.query[ProductRatePlan](SimpleFilter("ProductId", digitalProductPlan.id))

  private def productRatePlanCharges(productRatePlans: Seq[ProductRatePlan]): Future[Seq[ProductRatePlanCharge]] = {
    val clauses = productRatePlans.map(p => "ProductRatePlanId" -> p.id)
    client.query[ProductRatePlanCharge](OrFilter(clauses: _*))
  }

  private def productRatePlanChargeTiers(productRatePlanCharges: Seq[ProductRatePlanCharge]): Future[Seq[ProductRatePlanChargeTier]] = {
    val clauses = productRatePlanCharges.map(p => "ProductRatePlanChargeId" -> p.id)
    client.query[ProductRatePlanChargeTier](OrFilter(clauses: _*))
  }

  private def getSubscriptions: Future[Seq[SubscriptionProduct]] = {

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
}
