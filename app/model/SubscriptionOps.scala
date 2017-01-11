package model

import com.gu.memsub.{OneOffPeriod, Product, Weekly}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, Subscription}
import com.gu.memsub.subsv2.SubscriptionPlan.{Paid, PaperPlan, WeeklyPlan}
import com.github.nscala_time.time.OrderingImplicits._
import org.joda.time.LocalDate.now

object SubscriptionOps {

  type WeeklyPlanOneOff = PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, OneOffPeriod]]

  implicit class EnrichedPaidSubscription[P <: Paid](subscription: Subscription[P]) {
    val currency = subscription.plans.head.charges.currencies.head
    val currentOrExpiredPlan = subscription.plans.list.filterNot(_.start.isAfter(now)).maxBy(_.end)
    val latestPlan = subscription.plans.list.maxBy(_.end)
  }

  implicit class EnrichedPaperSubscription[P <: PaperPlan](subscription: Subscription[P]) {
    val renewable = subscription.latestPlan.charges.billingPeriod.isInstanceOf[OneOffPeriod]
  }

  implicit class EnrichedWeeklySubscription[P <: WeeklyPlan](subscription: Subscription[P]) {
    val asRenewable = if (subscription.renewable) Some(subscription.asInstanceOf[Subscription[WeeklyPlanOneOff]]) else None
  }
}
