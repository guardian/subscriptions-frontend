package model

import com.gu.memsub.{BillingPeriod, RecurringPeriod}

object BillingPeriodOps {

  implicit class EnrichedBillingPeriod(in: BillingPeriod) {
    def isRecurring = in match {
      case _: RecurringPeriod => true
      case _ => false
    }
  }

}
