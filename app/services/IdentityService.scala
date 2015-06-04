package services

import com.amazonaws.regions.{Regions, Region}
import com.gu.monitoring.{AuthenticationMetrics, RequestMetrics, StatusMetrics, CloudWatch}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Play.current
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class IdUser(id: String)

class IdentityService(identityApiClient: IdentityApiClient) {
  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] = identityApiClient.userLookupByScGuUCookie(cookieValue)
    .map(resp => jsonToIdUser(resp.json))

  def userLookupByEmail(email: String): Future[Option[IdUser]] = identityApiClient.userLookupByEmail(email)
    .map(resp => jsonToIdUser(resp.json \ "user"))

  def registerGuest(personalData: PersonalData): Future[Option[IdUser]] = identityApiClient.createGuest(JsObject(Map(
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
  .map(resp => jsonToIdUser(resp.json \ "user"))

  private def jsonToIdUser = (json: JsValue) => (json \ "id").asOpt[String].map(IdUser)
}

object IdentityService extends IdentityService(IdentityApiClient)

trait IdentityApiClient {
  def userLookupByScGuUCookie: String => Future[WSResponse]

  def createGuest: JsValue => Future[WSResponse]

  def userLookupByEmail: String => Future[WSResponse]
}

object IdentityApiClient extends IdentityApiClient with LazyLogging {

  lazy val identityEndpoint = Config.Identity.baseUri
  lazy val metrics = new IdApiMetrics(Config.stage)

  implicit class FutureWSLike(f: Future[WSResponse]) {
    /**
     * Example of ID API Response: `{"status":"error","errors":[{"message":"Forbidden","description":"Field access denied","context":"privateFields.billingA ddress1"}]}`
     * @return
     */
    private def applyOnSpecificErrors(reasons: List[String])(block: WSResponse => Unit): Unit = {
      def errorMessageFilter(error: JsValue): Boolean =
        (error \ "message").asOpt[JsString].filter(e => reasons.contains(e.value)).isDefined

      f.map(response =>
        (response.json \ "status").asOpt[String].filter(_ == "error")
          .foreach(_ => (response.json \ "errors").asOpt[JsArray].flatMap(_.value.headOption)
            .filter(errorMessageFilter)
            .foreach(_ => block(response))))
    }

    def withWSFailureLogging(endpoint: WSRequestHolder) = {
      f.onFailure {
        case e: Throwable =>
          logger.error("Connection error: " + endpoint.url, e)
      }
      applyOnSpecificErrors(List("Access Denied", "Forbidden"))(r => logger.error(r.body))
      f
    }

    def withCloudwatchMonitoring(method: String) = {
      f.map(response => metrics.recordResponse(response.status, method))

      applyOnSpecificErrors(List("Access Denied", "Forbidden"))(_ => metrics.recordAuthenticationError)

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