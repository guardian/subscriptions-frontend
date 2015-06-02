package services

import com.amazonaws.regions.{Regions, Region}
import com.gu.monitoring.{AuthenticationMetrics, RequestMetrics, StatusMetrics, CloudWatch}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Play.current
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class IdUser(id: String)

class IdentityService(identityApiClient: IdentityApiClient) {
  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] = identityApiClient.userLookupByScGuUCookie(cookieValue)
    .map(resp => jsonToIdUser(resp.json))

  def userLookupByEmail(email: String): Future[Option[IdUser]] = identityApiClient.userLookupByEmail(email)
    .map(resp => jsonToIdUser(resp.json \ "user"))

  def registerGuest(personalData: PersonalData) = identityApiClient.createGuest(JsObject(Map(
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

object IdentityService extends IdentityService(IdentityApiClient)

trait IdentityApiClient {
  def userLookupByScGuUCookie: String => Future[WSResponse]

  def createGuest: JsValue => Future[WSResponse]

  def userLookupByEmail: String => Future[WSResponse]
}

object IdentityApiClient extends IdentityApiClient with LazyLogging {

  val identityEndpoint = Config.Identity.baseUri
  lazy val metrics = new IdApiMetrics(Config.stage)

  implicit class FutureWSLike(f: Future[WSResponse]) {
    def withWSFailureLogging(endpoint: WSRequestHolder) = {
      f.onFailure {
        case e: Throwable =>
          logger.error("Connection error: " + endpoint.url, e)
      }
      f
    }

    def withCloudwatchMonitoring(method: String) = {
      f.map(response => metrics.recordResponse(response.status, method))
      f.map { response =>
        (response.json \ "status").asOpt[String].filter(_ == "error")
          .flatMap(_ => (response.json \\ "errors").find(_ \ "message" == "Access Denied"))
          .foreach(_ => metrics.recordAuthenticationError)
      }
      f
    }

    def withCloudwatchMonitoringOfPost = withCloudwatchMonitoring("POST")

    def withCloudwatchMonitoringOfGet = withCloudwatchMonitoring("GET")
  }

  private def authoriseCall = (wsHolder: WSRequestHolder) =>
    wsHolder.withHeaders(("Authorization", s"Bearer ${Config.Identity.apiToken}"))

  def userLookupByEmail: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user"))

    email => endpoint.withQueryString(("emailAddress", email)).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  def userLookupByScGuUCookie: String => Future[WSResponse] = {
    val endpoint = WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/"))

    cookieValue => endpoint.withHeaders(("Cookie", s"SC_GU_U=$cookieValue;")).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  def createGuest: JsValue => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest"))

    guestJson => endpoint.post(guestJson)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPost
  }
}

class IdApiMetrics(val stage: String) extends CloudWatch
  with StatusMetrics
  with RequestMetrics
  with AuthenticationMetrics {

  val region = Region.getRegion(Regions.EU_WEST_1)
  val application = Config.appName
  val service = "Identity"

  def recordRequest = putRequest

  def recordResponse(status: Int, responseMethod: String) = putResponseCode(status, responseMethod)

  def recordAuthenticationError = putAuthenticationError
}