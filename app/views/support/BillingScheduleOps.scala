package views.support
import com.gu.memsub.BillingSchedule
import com.gu.memsub.BillingSchedule.Bill
import org.joda.time.LocalDate.now

import scalaz.syntax.std.boolean._

object BillingScheduleOps {
  implicit class InvoiceOps(bill: Bill) {
    def whenDiscounted(str: String): Option[String] =
      (bill.accountCredit.isDefined|| bill.items.list.exists(_.amount < 0)).option(str)
  }

  implicit class BillingScheduleOps(billingSchedule: BillingSchedule) {
    def billsToDisplay = {
      val amountOfBillsInNext12Months = billingSchedule.invoices.list.count(_.date.isBefore(now.plusYears(1)))
      val maxBills = Math.min(amountOfBillsInNext12Months + 1, 13)
      billingSchedule.invoices.list.take(maxBills)
    }
  }
}
