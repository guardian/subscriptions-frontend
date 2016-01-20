package views.support

import com.gu.subscriptions.CAS.{CASError, CASSuccess}
import play.api.libs.json._

object CASResultOps {
  implicit val casSuccessWrites = Json.writes[CASSuccess]
  implicit val casErrorWrites = Json.writes[CASError]
}
