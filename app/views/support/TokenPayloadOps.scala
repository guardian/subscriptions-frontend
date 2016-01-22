package views.support

import com.gu.cas.{PrefixedTokens, TokenPayload}
import play.api.libs.json._

object TokenPayloadOps {
  implicit val tokenPayloadWrites = new Writes[TokenPayload] {
    override def writes(p: TokenPayload) = {
      val expiryDate = TokenPayload.epoch
        .plusDays(p.creationDateOffset.getDays)
        .plusWeeks(p.period.getWeeks)
        .toLocalDate.toString("yyy-MM-dd")

      Json.obj(
        "creationDateOffset" -> p.creationDateOffset.getDays,
        "period" -> p.period.getWeeks,
        "subscriptionCode" -> p.subscriptionCode.toString,
        "expiryType" -> "sub",
        "expiryDate" -> expiryDate
      )
    }
  }

  implicit class TokenPayloadToToken(tokenPayload: TokenPayload)(implicit encoder: PrefixedTokens) {
    def token: String = encoder.encode(tokenPayload)
    def obfuscatedToken: String = "*" * (token.length - 4) + token.takeRight(4)
  }
}
