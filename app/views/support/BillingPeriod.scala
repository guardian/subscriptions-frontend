package views.support
import com.gu.memsub.{Month, OneYear, Quarter, RecurringPeriod, Year, BillingPeriod => BP}

object BillingPeriod {
  implicit class BillingPeriodOps(billingPeriod: BP)  {
    def frequencyInMonths = billingPeriod match {
      case Month() => "every month"
      case Quarter() => "every 3 months"
      case Year() => "every 12 months"
      case OneYear() => "one off payment"
    }

    def isRecurring = billingPeriod match {
      case _: RecurringPeriod => true
      case _ => false
    }

  }



}
