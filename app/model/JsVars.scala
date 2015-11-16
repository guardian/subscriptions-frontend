package model

import play.api.libs.json._

object JsVars {
  def default = JsVars(
    userIsSignedIn = false,
    ignorePageLoadTracking = false,
    stripePublicKey = None
  )

  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = {
      val mandatory = Json.obj(
        "user" -> Json.obj(
          "isSignedIn" -> jsVars.userIsSignedIn,
          "ignorePageLoadTracking" -> jsVars.ignorePageLoadTracking
        )
      )

      val optional = jsVars.stripePublicKey match {
        case Some(stripePublicKey) => Json.obj("stripePublicKey" -> stripePublicKey)
        case None => Json.obj()
      }

      mandatory ++ optional
    }
  }
}

case class JsVars(
  userIsSignedIn: Boolean,
  ignorePageLoadTracking: Boolean,
  stripePublicKey: Option[String]
)
