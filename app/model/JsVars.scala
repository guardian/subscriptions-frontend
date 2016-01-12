package model

import com.gu.i18n.{GBP, Currency, CountryGroup}
import play.api.libs.json._

object JsVars {
  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = {
      val mandatory = Json.obj(
        "user" -> Json.obj(
          "isSignedIn" -> jsVars.userIsSignedIn,
          "ignorePageLoadTracking" -> jsVars.ignorePageLoadTracking
        ),
        "currency" -> jsVars.currency.toString,
        "country" -> jsVars.countryGroup.defaultCountry.map(_.alpha2)
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
  userIsSignedIn: Boolean = false,
  ignorePageLoadTracking: Boolean = false,
  stripePublicKey: Option[String] = None,
  countryGroup: CountryGroup = CountryGroup.UK,
  currency: Currency = GBP
)
