package utils

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.mvc.Request

object RequestCountry {
  implicit class RequestWithFastlyCountry(r: Request[_]) {
    def getFastlyCountry = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.byFastlyCountryCode)
    def toUriWithCampaignParams(defaultUri: Uri) = Seq("INTCMP", "CMP", "mcopy").foldLeft[Uri](defaultUri) { (url, param) =>
      // No need to filter out the params that aren't present because if the `?` method gets a key-value tuple
      // with value of None, that parameter will not be rendered when toString is called
      url ? (param -> r.getQueryString(param))
    }
  }
}

