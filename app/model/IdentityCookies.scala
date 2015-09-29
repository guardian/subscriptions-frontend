package model

import org.joda.time.{DateTime, Duration}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Cookie

import scala.util.Try

case class IdentityCookies(guu: Cookie, scguu: Cookie)

object IdentityCookies {
  def fromGuestConversion(payload: String): Option[IdentityCookies] = {
    implicit val cookieRead: Reads[Cookie] = (
      (JsPath \ "key").read[String] and
        (JsPath \ "value").read[String]
      )(Cookie.apply(_, _))

    for {
      json <- Try { Json.parse(payload)}.toOption
      expirationString <- (json \ "cookies" \ "expiresAt").asOpt[String]
      expiration <- Try { new DateTime(expirationString) }.toOption
      maxAge = new Duration(DateTime.now, expiration).getStandardSeconds.toInt
      cookies <- (json \ "cookies" \ "values").asOpt[Seq[Cookie]]
      guuCookie <- cookies.find(_.name == "GU_U")
      scguuCookie <- cookies.find(_.name == "SC_GU_U")
    } yield {
      IdentityCookies(
        guuCookie.copy(maxAge = Some(maxAge)),
        scguuCookie.copy(maxAge = Some(maxAge), secure = true)
      )
    }
  }
}
