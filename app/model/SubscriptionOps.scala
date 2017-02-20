package model

import com.gu.memsub.{Product, Weekly}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, Subscription, SubscriptionPlan}
import com.gu.memsub.subsv2.SubscriptionPlan.{Paid, PaperPlan, WeeklyPlan}
import com.github.nscala_time.time.OrderingImplicits._
import com.gu.i18n.Currency
import com.gu.memsub.BillingPeriod.{OneOffPeriod, SixWeeks}
import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub.promo.{NewUsers, ValidPromotion}
import com.typesafe.scalalogging.LazyLogging
import controllers.ContextLogging
import org.joda.time.LocalDate.now
import model.BillingPeriodOps._
import views.support.Pricing._
import scalaz.syntax.std.boolean._

object SubscriptionOps extends LazyLogging {

  type WeeklyPlanOneOff = PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, OneOffPeriod]]

  implicit class EnrichedPaidSubscription[P <: Paid](subscription: Subscription[P]) {

    private def containsPlanFor(p:Product):Boolean = subscription.plans.list.exists(_.product == p)
//TODO IT'S NOT TRIVIAL TO GET A SUBSCRIPTION TO THE THANK YUOU PAGE SO THIS WILL PROBABLY NOT BE USED
//    def isHomeDelivery: Boolean = containsPlanFor(Delivery)
//
//    def isVoucher: Boolean = containsPlanFor(Voucher)
//
//    def isDigitalPack: Boolean = containsPlanFor(com.gu.memsub.Product.Digipack)
//
//    def isGuardianWeekly: Boolean = subscription.plans.list.exists(_ match {
//      case _:Product.Weekly => true
//      case _ => false
//    })
//
//    def email: String = (
//      subscription.isHomeDelivery.option("homedelivery@theguardian.com") orElse
//        subscription.isVoucher.option("vouchersubs@theguardian.com") orElse
//        subscription.isDigitalPack.option("digitalpack@theguardian.com") orElse
//        subscription.isGuardianWeekly.option("gwsubs@theguardian.com")
//      ).getOrElse("subscriptions@theguardian.com")


    val currency = subscription.plans.head.charges.currencies.head
    val nextPlan = subscription.plans.list.maxBy(_.end)
    val planToManage = {
      lazy val coveringPlans = subscription.plans.list.filter(p => (p.start.isEqual(now) || p.start.isBefore(now)) && p.end.isAfter(now))
      lazy val expiredPlans = subscription.plans.list.filter(p => p.end.isBefore(now) || p.end.isEqual(now))

      // The user's sub might be in many situations:

      if (coveringPlans.nonEmpty) {
        // Has a plan which is currently active (i.e today sits within the plan's start and end dates)
        coveringPlans.maxBy(_.end)
      } else if (expiredPlans.nonEmpty) {
        // Has a plan which has recently expired
        expiredPlans.maxBy(_.end)
      } else {
        // Has a plan(s) starting soon
        if (subscription.plans.size > 1) {
          logger.warn("User has multiple future plans, we are showing them their last ending plan")
        }
        subscription.plans.list.minBy(_.start)
      }
    }

    //todo ask if we need this
    val nonExpiredSubscriptions = subscription.plans.list.filter(_.end.isAfter(now))


    val recurringPlans = nonExpiredSubscriptions.filter(_.charges.billingPeriod.isRecurring)
    val oneOffPlans = nonExpiredSubscriptions.filterNot(_.charges.billingPeriod.isRecurring)
    val introductoryPeriodPlan = oneOffPlans.find(_.charges.billingPeriod == SixWeeks)
    val hasIntroductoryPeriod = introductoryPeriodPlan.isDefined


    //TODO MAYBE THIS ONLY MAKES SENSE FOR NEW SUBS SO IT SHOULD BE MOVED TO EXACTTARGET SERVICE?
    //TODO ALSO THIS STILL USES SUBSCRIPTION.PLAN
    def newSubPaymentDescripttion(validPromotion: Option[ValidPromotion[NewUsers]], currency: Currency): String = {

      val discountedPlanDescription = (for {
        vp <- validPromotion
        discountPromotion <- vp.promotion.asDiscount
      } yield {
        subscription.plan.charges.prettyPricingForDiscountedPeriod(discountPromotion, currency)
      })

      val introductoryPeriodSubDescription = subscription.introductoryPeriodPlan.map{introductoryPlan =>

        val nextRecurrringPeriod = subscription.recurringPlans.minBy(_.start)

        introductoryPlan.charges.prettyPricing(currency) + " then " + nextRecurrringPeriod.charges.prettyPricing(currency)
      }

      discountedPlanDescription orElse introductoryPeriodSubDescription getOrElse subscription.plan.charges.prettyPricing(currency)
    }
  }

  implicit class EnrichedPaperSubscription[P <: PaperPlan](subscription: Subscription[P]) extends ContextLogging {
    implicit val subContext = subscription
    val renewable = {
      val wontAutoRenew = !subscription.autoRenew
      val startedAlready = !subscription.termStartDate.isAfter(now)
      val isOneOffPlan = !subscription.planToManage.charges.billingPeriod.isRecurring
      info(s"testing if renewable - wontAutoRenew: $wontAutoRenew, startedAlready: $startedAlready, isOneOffPlan: $isOneOffPlan")
      wontAutoRenew && startedAlready && isOneOffPlan
    }
  }

  implicit class EnrichedWeeklySubscription[P <: WeeklyPlan](subscription: Subscription[P]) {
    val asRenewable = if (subscription.renewable) Some(subscription.asInstanceOf[Subscription[WeeklyPlanOneOff]]) else None
    val secondPaymentDate = if (subscription.hasIntroductoryPeriod) subscription.acceptanceDate else subscription.acceptanceDate plusMonths subscription.plan.charges.billingPeriod.monthsInPeriod

  }
}
