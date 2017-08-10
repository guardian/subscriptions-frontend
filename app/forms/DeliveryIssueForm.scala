package forms

import com.gu.memsub.Subscription.Name
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._

object TrackDeliveryForm {

  val lookup: Form[TrackDelivery] = Form(
    mapping(
      "subscriptionName" -> nonEmptyText.transform[Name](Name, _.get),
      "issueDate" -> jodaLocalDate("dd MMMM yyyy")
    )(TrackDelivery.apply)(TrackDelivery.unapply)
  )

}

case class TrackDelivery(subscriptionName: Name, issueDate: LocalDate)
