package model
import play.api.libs.json._
import controllers.CachedAssets.hashedPathFor

object JsVars {
  def default = JsVars(
    userIsSignedIn = false,
    resourcePaths = Map("zxcvbn" -> hashedPathFor("javascripts/vendor/zxcvbn.js"))
  )

  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = Json.obj(
      "resourcePaths" -> jsVars.resourcePaths,
      "user" -> Json.obj(
        "isSignedIn" -> jsVars.userIsSignedIn
      )
    )
  }
}
case class JsVars(userIsSignedIn: Boolean, resourcePaths: Map[String, String])
