package services

import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequestHolder, WSResponse, WS}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object IdentityService {

  case class IdUser(id: String)

  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] = IdentityApiClient.userLookupByScGuUCookie(cookieValue)
    .map(resp => jsonToIdUser(resp.json))

  def userLookupByEmail(email: String): Future[Option[IdUser]] = IdentityApiClient.userLookupByEmail(email)
    .map(resp => jsonToIdUser(resp.json))

  private def jsonToIdUser(json: JsValue): Option[IdUser] = (json \ "id").asOpt[String].map(IdUser)
}


object IdentityApiClient extends LazyLogging {

  val identityEndpoint = Config.Identity.baseUri

  val userLookupByEmailWS = WS.url(s"$identityEndpoint/user").withHeaders(("Authorization", s"Bearer ${Config.Identity.apiToken}"))
  val userLookupByScGuUCookie = WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/"))

  def userLookupByEmail(email: String): Future[WSResponse] = {
    val response: Future[WSResponse] = userLookupByEmailWS.withQueryString(("emailAddress", email)).execute()
    response.onFailure {
      case e: Throwable =>
        logger.error("ID API connection error", e)
    }
    response.onSuccess {
      case x => println(s"userLookupByEmail: ${x.body}")
    }
    response
  }

  def userLookupByScGuUCookie(cookieValue: String): Future[WSResponse] = {
    val response: Future[WSResponse] = userLookupByScGuUCookie.withHeaders(("Cookie", s"SC_GU_U=$cookieValue;")).execute()
    response.onFailure {
      case e: Throwable =>
        logger.error("ID API connection error", e)
    }
    response.onSuccess {
      case x => println(s"userLookupByScGuUCookie: ${x.body}")
    }
    response
  }
}
