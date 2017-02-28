package views.support
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.{BillingPeriod => BP}

object BillingPeriod {
  implicit class BillingPeriodOps(billingPeriod: BP)  {
    def frequencyInMonths = billingPeriod match {
      case Month => "every month"
      case Quarter => "every 3 months"
      case Year => "every 12 months"
      case period: OneOffPeriod => s"one off payment to cover ${period.noun}"
    }
  }
}
