package utils

import com.gu.i18n.CountryGroup
import play.api.mvc.Request

object RequestCountry {
  implicit class RequestWithFastlyCountry(r: Request[_]) {
    def getFastlyCountryGroup = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.byFastlyCountryCode)
    def getFastlyCountry = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.countryByCode)
  }
}

