package views.support
import com.gu.memsub.{Year, Quarter, Month, BillingPeriod => BP}

object BillingPeriod {
  implicit class BillingPeriodOps(billingPeriod: BP)  {
    def frequencyInMonths = billingPeriod match {
      case Month() => "every month"
      case Quarter() => "every 3 months"
      case Year() => "every 12 months"
    }

    def numberOfMonths = billingPeriod match {
      case Month() => 1
      case Quarter() => 3
      case Year() => 12
    }
  }
}
