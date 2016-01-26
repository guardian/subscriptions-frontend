package forms

import com.gu.cas.{SubscriptionCode, TokenPayload}
import com.gu.memsub.Subscription.Name
import model.CASLookup
import org.joda.time.Weeks
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formatter

object CASForm {
  val lookup: Form[CASLookup] = Form(
    "cas" -> mapping(
      "number" -> nonEmptyText.transform[Name](Name, _.get),
      "password" -> nonEmptyText
    )(CASLookup.apply)(CASLookup.unapply)
  )

  private implicit val subscriptionCode = new Formatter[SubscriptionCode] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], SubscriptionCode] =
      for {
        str <- data.get(key).toRight(Seq(FormError(key, s"Not found"))).right
        code <- SubscriptionCode.all.find(_.toString == str)
                  .toRight(Seq(FormError(key, s"Subscription code should be one of ${SubscriptionCode.all.mkString(", ")}")))
                  .right
      } yield code

    override def unbind(key: String, value: SubscriptionCode): Map[String, String] =
      Map(key -> value.toString)
  }

  val emergencyToken: Form[TokenPayload] = Form(
    "cas" -> mapping(
      "period" -> number(min = 1, max = 13).transform[Weeks](Weeks.weeks, _.getWeeks),
      "subscriptionCode" -> of[SubscriptionCode]
    )(TokenPayload.apply)(t => Some(t.period, t.subscriptionCode))
  )
}
