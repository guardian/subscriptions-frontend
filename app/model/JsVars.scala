package model
import play.api.libs.json._

object JsVars {
  def default = JsVars(userIsSignedIn = false)

  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = Json.obj(
      "user" -> Json.obj(
        "isSignedIn" -> jsVars.userIsSignedIn
      )
    )
  }
}
case class JsVars(userIsSignedIn: Boolean)
