package views.support

import com.gu.subscriptions.suspendresume.SuspensionService.{AlreadyOnHoliday, BadZuoraJson, NegativeDays, NoRefundDue, NotEnoughNotice, RefundError}

object AccountManagementOps {
  implicit class RefundErrorMessage(in: RefundError) {
    def getMessage: String = in match {
      case NoRefundDue => "You aren't receiving the paper on any of the days you've selected. Please select an appropriate date range and try again."
      case NotEnoughNotice => "Unfortunately we require five days notice to suspend deliveries. Please select a later date range and try again."
      case AlreadyOnHoliday => "It looks like you're already on holiday sometime during that date range. Please select a date range before or after your existing holiday, and try again."
      case NegativeDays => "Invalid date range. Please select an appropriate date range and try again."
      case _ => "Unexpected error. Please try again."
    }
  }
  def getMessageFromCode(code: String): String = {
    code match {
      case NoRefundDue.code => NoRefundDue.getMessage
      case NotEnoughNotice.code => NotEnoughNotice.getMessage
      case AlreadyOnHoliday.code => AlreadyOnHoliday.getMessage
      case NegativeDays.code => NegativeDays.getMessage
      case _ => BadZuoraJson("Unexpected error").getMessage
    }
  }
}
