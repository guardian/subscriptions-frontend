package forms

import com.gu.memsub.Subscription.Name
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._

object ReportDeliveryProblemForm {

  val report: Form[ReportDeliveryProblem] = Form(
    mapping(
      "subscriptionName" -> nonEmptyText.transform[Name](Name, _.get),
      "sfContactId" -> nonEmptyText,
      "issueDate" -> jodaLocalDate("dd MMMM yyyy")
    )(ReportDeliveryProblem.apply)(ReportDeliveryProblem.unapply)
  )

}

case class ReportDeliveryProblem(subscriptionName: Name, sfContactId: String, issueDate: LocalDate)
