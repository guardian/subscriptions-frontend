package model
import play.api.libs.json._

object JsVars {
  def default = JsVars(userIsSignedIn = false, ignorePageLoadTracking = false)

  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = Json.obj(
      "user" -> Json.obj(
	"isSignedIn" -> jsVars.userIsSignedIn,
	"ignorePageLoadTracking" -> jsVars.ignorePageLoadTracking
      )
    )
  }
}
case class JsVars(userIsSignedIn: Boolean, ignorePageLoadTracking : Boolean)
