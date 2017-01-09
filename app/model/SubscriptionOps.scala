package model

import com.gu.memsub.{OneOffPeriod, Product, Weekly}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, Subscription}
import com.gu.memsub.subsv2.SubscriptionPlan.PaperPlan
import com.github.nscala_time.time.OrderingImplicits._
import org.joda.time.LocalDate

object SubscriptionOps {

  type WeeklyPlanOneOff = PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, OneOffPeriod]]

  implicit class EnrichedSubscription[P <: PaperPlan](subscription: Subscription[P]) {
    val latestPlan = {
      def getLatest(p1: P, p2: P) = if (p1.end.isAfter(p2.end)) p1 else p2
      subscription.plans.list.reduceLeft(getLatest)
    }
    val renewable = latestPlan.charges.billingPeriod.isInstanceOf[OneOffPeriod]
    val asRenewable = if (renewable) Some(subscription.asInstanceOf[Subscription[WeeklyPlanOneOff]]) else None
  }

  implicit class EnrichedRenewableSubscription[P <: WeeklyPlanOneOff](subscription: Subscription[P]) {
    val renewalDate: LocalDate = {
      // Is the latest of: the term end date, or the fixed period plus the number of months in the period (usually just 12)
      val fixedPeriodEnds = subscription.plan.start.plusMonths(subscription.plan.charges.billingPeriod.monthsInPeriod)
      Seq(fixedPeriodEnds, subscription.termEndDate).max
    }
  }

}
