package model

import com.gu.i18n.Currency.GBP
import com.gu.i18n.{Country, Currency}
import play.api.libs.json._

object JsVars {
  implicit val jsVarsWrites = new Writes[JsVars] {
    def writes(jsVars: JsVars) = {
      val mandatory = Json.obj(
        "user" -> Json.obj(
          "isSignedIn" -> jsVars.userIsSignedIn,
          "ignorePageLoadTracking" -> jsVars.ignorePageLoadTracking
        ),
        "currency" -> jsVars.currency.toString
      )

      val optionalCountry = Json.obj("country" -> jsVars.country.map(_.alpha2))


      mandatory ++ optionalCountry
    }
  }
}

case class JsVars(
  userIsSignedIn: Boolean = false,
  ignorePageLoadTracking: Boolean = false,
  currency: Currency = GBP,
  country: Option[Country] = None
)
