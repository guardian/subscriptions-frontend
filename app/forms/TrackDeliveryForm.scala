package forms

import com.gu.memsub.Subscription.Name
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._

object TrackDeliveryForm {

  val lookup: Form[TrackDeliveryRequest] = Form(
    mapping(
      "subscriptionName" -> nonEmptyText.transform[Name](Name, _.get),
      "sfContactId" -> nonEmptyText,
      "issueDate" -> jodaLocalDate("dd MMMM yyyy")
    )(TrackDeliveryRequest.apply)(TrackDeliveryRequest.unapply)
  )

}

case class TrackDeliveryRequest(subscriptionName: Name, sfContactId: String, issueDate: LocalDate)
