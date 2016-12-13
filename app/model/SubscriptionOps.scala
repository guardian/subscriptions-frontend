package model

import com.gu.memsub.OneOffPeriod
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.PaperPlan

object SubscriptionOps {

  implicit class EnrichedSubscription[P <: PaperPlan](subscription: Subscription[P]) {
    val latestPlan = {
      def getLatest(p1: P, p2: P) = if (p1.end.isAfter(p2.end)) p1 else p2
      subscription.plans.list.reduceLeft(getLatest)
    }
    val renewable = latestPlan.charges.billingPeriod.isInstanceOf[OneOffPeriod]
  }
}
