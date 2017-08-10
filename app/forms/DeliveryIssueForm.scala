package forms

import com.gu.memsub.Subscription.Name
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._

object DeliveryIssueForm {

  val lookup: Form[DeliveryIssue] = Form(
    mapping(
      "subscriptionName" -> nonEmptyText.transform[Name](Name, _.get),
      "issueDate" -> jodaLocalDate("dd MMMM yyyy")
    )(DeliveryIssue.apply)(DeliveryIssue.unapply)
  )

}

case class DeliveryIssue(subscriptionName: Name, issueDate: LocalDate)
