package utils

import com.gu.identity.play._
import play.api.libs.json._

object TestIdUser {
  implicit val writesStatusFields = Json.writes[StatusFields]
  implicit val writesTelephoneNumber = Json.writes[TelephoneNumber]
  implicit val writesPrivateFields = Json.writes[PrivateFields]
  implicit val writesPublicFields = Json.writes[PublicFields]
  implicit val writesIdUser = Json.writes[IdUser]

  val testUser = IdUser(
    id = "test-user",
    primaryEmailAddress = "test-user@example.com",
    publicFields = PublicFields(None),
    privateFields = None,
    statusFields = None
  )
}
