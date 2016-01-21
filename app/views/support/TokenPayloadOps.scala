package views.support

import com.gu.cas.TokenPayload
import play.api.libs.json._

object TokenPayloadOps {
  implicit val tokenPayloadWrites = new Writes[TokenPayload] {
    override def writes(p: TokenPayload) = Json.obj(
      "creationDateOffset" -> p.creationDateOffset.getDays,
      "period" -> p.period.getWeeks,
      "subscriptionCode" -> p.subscriptionCode.toString
    )
  }
}
