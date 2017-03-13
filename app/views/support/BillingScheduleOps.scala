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
      val bills = billingSchedule.invoices.list
      val amountOfBillsInNext12Months = bills.count(_.date.isBefore(now.plusYears(1)))
      bills.take(amountOfBillsInNext12Months + 1)
    }
  }
}
