package forms

import com.gu.memsub.Subscription.Name
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._

object ReportDeliveryIssueForm {

  val issueReport: Form[ReportDeliveryIssue] = Form(
    mapping(
      "subscriptionName" -> nonEmptyText.transform[Name](Name, _.get),
      "sfContactId" -> nonEmptyText,
      "issueDate" -> jodaLocalDate("dd MMMM yyyy")
    )(ReportDeliveryIssue.apply)(ReportDeliveryIssue.unapply)
  )

}

case class ReportDeliveryIssue(subscriptionName: Name, sfContactId: String, issueDate: LocalDate)
