package model

import play.api.libs.json._

object JsVars {
  def default = JsVars()

  implicit val jsVarsWrites = new Writes[JsVars] {
    private val innerWrites = Json.writes[JsVars]
    def writes(jsVars: JsVars) = Json.obj(
      "user" -> Json.toJson(jsVars)(innerWrites)
    )
  }
}

case class JsVars(
  userIsSignedIn: Boolean = false,
  ignorePageLoadTracking: Boolean = false,
  stripePublicKey: Option[String] = None
)
