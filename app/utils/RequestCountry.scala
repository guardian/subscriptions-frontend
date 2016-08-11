package utils

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.mvc.Request

object RequestCountry {
  implicit class RequestWithFastlyCountry(r: Request[_]) {
    def getFastlyCountry = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.byFastlyCountryCode)
  }
}

