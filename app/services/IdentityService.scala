package services

import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Play.current
import play.api.libs.json.Json._
import play.api.libs.json.{JsString, JsObject, Json, JsValue}
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object IdentityService {

  case class IdUser(id: String)

  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] = IdentityApiClient.userLookupByScGuUCookie(cookieValue)
    .map(resp => jsonToIdUser(resp.json))

  def userLookupByEmail(email: String): Future[Option[IdUser]] = IdentityApiClient.userLookupByEmail(email)
    .map(resp => jsonToIdUser(resp.json \ "user"))

  def registerGuest(personalData: PersonalData) = IdentityApiClient.createGuest(JsObject(Map(
    "primaryEmailAddress" -> JsString(personalData.email),
    "privateFields" -> JsObject(Map(
      "firstName" -> JsString(personalData.firstName),
      "secondName" -> JsString(personalData.lastName),
      "billingAddress1" -> JsString(personalData.address.house),
      "billingAddress2" -> JsString(personalData.address.street),
      "billingAddress3" -> JsString(personalData.address.town),
      "billingPostcode" -> JsString(personalData.address.postcode),
      "billingCountry" -> JsString("United Kingdom")
    ).toSeq),
    "statusFields" -> JsObject(Map("receiveGnmMarketing" -> JsString("true")).toSeq)
  ).toSeq))

  private def jsonToIdUser = (json: JsValue) => (json \ "id").asOpt[String].map(IdUser)
}


object IdentityApiClient extends LazyLogging {

  val identityEndpoint = Config.Identity.baseUri

  implicit class FutureWSLike(f: Future[WSResponse]) {
    def withWSFailureLogging(endpoint: WSRequestHolder) = {
      f.onFailure {
        case e: Throwable =>
          logger.error("Connection error: " + endpoint.url, e)
      }
      f
    }
  }

  private def authoriseCall = (wsHolder: WSRequestHolder) => wsHolder.withHeaders(("Authorization", s"Bearer ${Config.Identity.apiToken}"))

  def userLookupByEmail: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user"))

    email => endpoint.withQueryString(("emailAddress", email)).execute().withWSFailureLogging(endpoint)
  }

  def userLookupByScGuUCookie: String => Future[WSResponse] = {
    val endpoint = WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/"))

    cookieValue => endpoint.withHeaders(("Cookie", s"SC_GU_U=$cookieValue;")).execute().withWSFailureLogging(endpoint)
  }

  def createGuest: JsValue => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest"))

    guestJson => endpoint.post(guestJson).withWSFailureLogging(endpoint)
  }
}
