package views.support
import com.gu.memsub.BillingSchedule.Bill
import scalaz.syntax.std.boolean._

object BillingScheduleOps {
  implicit class InvoiceOps(bill: Bill) {
    def isDiscounted(str: String): Option[String] =
      (bill.accountCredit.isDefined || bill.items.list.exists(_.amount < 0)).option(str)
  }
}
