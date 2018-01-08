package model

import com.github.nscala_time.time.OrderingImplicits._
import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod.{OneOffPeriod, SixWeeks}
import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub._
import com.gu.memsub.subsv2.SubscriptionPlan.{Paid, PaperPlan, WeeklyPlan}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, Subscription}
import com.typesafe.scalalogging.LazyLogging
import logging.{Context, ContextLogging}
import model.BillingPeriodOps._
import org.joda.time.LocalDate.now
import PartialFunction.cond

object SubscriptionOps extends LazyLogging {

  type WeeklyPlanOneOff = PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, OneOffPeriod]]

  implicit class EnrichedPaidSubscription[P <: Paid](subscription: Subscription[P]) {

    def isHomeDelivery: Boolean = containsPlanFor(Delivery)

    def isVoucher: Boolean = containsPlanFor(Voucher)

    def isDigitalPack: Boolean = containsPlanFor(Product.Digipack)

    def isGuardianWeekly = nonExpiredPlans.exists(plan => cond(plan.product) { case _: Product.Weekly => true })

    private def containsPlanFor(p:Product):Boolean = nonExpiredPlans.exists(_.product == p)

    def hasDigitalPack: Boolean = nonExpiredPlans.exists(_.charges.benefits.list.contains(Digipack))

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
        nextPlan
      }
    }

    def firstPrice = firstPlan.charges.price.getPrice(currency).map(_.amount).getOrElse(0F)
    def firstPlan = sortedPlans.head
    def firstProduct = firstPlan.product
    def currentPlans = subscription.plans.list.filter(p => (p.start == now || p.start.isBefore(now)) && p.end.isAfter(now))
    def futurePlans = subscription.plans.list.filter(_.start.isAfter(now) ).sortBy(_.start)
    def nonExpiredPlans = subscription.plans.list.filter(_.end.isAfter(now))
    def sortedPlans = subscription.plans.list.sortBy(_.start)
    def recurringPlans = subscription.plans.list.filter(p => p.end.isAfter(now) && p.charges.billingPeriod.isRecurring)
    def oneOffPlans = subscription.plans.list.filterNot(p => p.end.isBefore(now) || p.charges.billingPeriod.isRecurring)
    def introductoryPeriodPlan = oneOffPlans.find(_.charges.billingPeriod == SixWeeks)
    def hasIntroductoryPeriod = introductoryPeriodPlan.isDefined
  }

  implicit class EnrichedPaperSubscription[P <: PaperPlan](subscription: Subscription[P]) extends ContextLogging {
    def renewable(implicit context: Context) = {
      val wontAutoRenew = !subscription.autoRenew
      val startedAlready = !subscription.termStartDate.isAfter(now)
      info(s"testing if renewable - wontAutoRenew: $wontAutoRenew, startedAlready: $startedAlready")
      wontAutoRenew && startedAlready
    }
  }

  implicit class EnrichedWeeklySubscription[P <: WeeklyPlan](subscription: Subscription[P]) {
    def asRenewable(implicit context: Context) = if (subscription.renewable) Some(subscription.asInstanceOf[Subscription[WeeklyPlanOneOff]]) else None
    def secondPaymentDate = if (subscription.hasIntroductoryPeriod) subscription.acceptanceDate else subscription.acceptanceDate plusMonths subscription.plan.charges.billingPeriod.monthsInPeriod

  }
}
