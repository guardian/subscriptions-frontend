package views.support
import com.gu.memsub.BillingSchedule.Bill
import scalaz.syntax.std.boolean._

object BillingScheduleOps {
  implicit class InvoiceOps(bill: Bill) {
    def whenDiscounted(str: String): Option[String] =
      (bill.accountCredit.exists(_ > 0) || bill.items.list.exists(_.amount < 0)).option(str)
  }
}
