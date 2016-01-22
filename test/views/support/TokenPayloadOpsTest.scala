package views.support

import com.gu.cas.{PrefixedTokens, SevenDay, TokenPayload}
import org.joda.time.{Weeks, Days}
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import TokenPayloadOps._

class TokenPayloadOpsTest extends Specification {
  val tokenPayload = TokenPayload(creationDateOffset = Days.days(2), period = Weeks.weeks(1), subscriptionCode = SevenDay)

  "TokenPayload serialization" should {
    "return the basic TokenPayload fields and compute an expiry date" in {
      // The expiration date is 2012-09-20T00:00:00Z(TokenPayload.epoch) + creationDateOffset + period

      Json.toJson(tokenPayload) must_=== Json.obj(
        "creationDateOffset" -> 2,
        "period" -> 1,
        "subscriptionCode" -> SevenDay.toString,
        "expiryType" -> "sub",
        "expiryDate" -> "2012-09-29"
      )
    }
  }

  "TokenPayload is enriched" in {
    implicit object FakeEncoder extends PrefixedTokens("secret") {
      override def encode(payload: TokenPayload) = "GENERATEDTOKEN"
    }

    tokenPayload.token must_=== "GENERATEDTOKEN"
    tokenPayload.obfuscatedToken must_=== "**********OKEN"
  }
}
