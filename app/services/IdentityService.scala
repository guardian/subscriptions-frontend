package services

import com.amazonaws.regions.{Region, Regions}
import com.gu.identity.play.IdUser
import com.gu.monitoring.{AuthenticationMetrics, CloudWatch, RequestMetrics, StatusMetrics}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.http.Status
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class IdentityService(identityApiClient: IdentityApiClient) extends LazyLogging {
  import IdentityService.IdentityGuestPasswordError

  def doesUserExist(email: String): Future[Boolean] =
    identityApiClient.userLookupByEmail(email).map { response =>
      (response.json \ "user" \ "id").asOpt[String].isDefined
    }

  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] =
    identityApiClient.userLookupByScGuUCookie(cookieValue).map { response =>
      (response.json \ "user").asOpt[IdUser]
    }

 def registerGuest(personalData: PersonalData): Future[GuestUser] = {
    val json = JsObject(Map(
      "primaryEmailAddress" -> JsString(personalData.email),
      "statusFields" -> JsObject(Map("receiveGnmMarketing" -> JsBoolean(true))),
      "privateFields" -> JsObject(Map(
        "firstName" ->  JsString(personalData.firstName),
        "secondName" -> JsString(personalData.lastName),
        "billingAddress1" -> JsString(personalData.address.address1),
        "billingAddress2" -> JsString(personalData.address.address2),
        "billingAddress3" -> JsString(personalData.address.town),
        "billingPostcode" -> JsString(personalData.address.postcode),
        "billingCountry" -> JsString("United Kingdom")
    ))))

    identityApiClient.createGuest(json).map(response => response.json.as[GuestUser])
  }

  def convertGuest(password: String, token: IdentityToken): Future[Unit] = {
    val json = JsObject(Map("password" -> JsString(password)))
    identityApiClient.convertGuest(json, token).map { response =>
      if (response.status != Status.OK) {
        throw new IdentityGuestPasswordError(response.body)
      }
    }
  }
}

object IdentityService extends IdentityService(IdentityApiClient) {
  class IdentityGuestPasswordError(jsonMsg: String) extends RuntimeException(s"Cannot set password for Identity guest user. Json response form service: $jsonMsg")
}

trait IdentityApiClient {

  def userLookupByScGuUCookie: String => Future[WSResponse]

  def createGuest: JsValue => Future[WSResponse]

  def convertGuest: (JsValue, IdentityToken) => Future[WSResponse]

  def userLookupByEmail: String => Future[WSResponse]
}

object IdentityApiClient extends IdentityApiClient with LazyLogging {

  lazy val identityEndpoint = Config.Identity.baseUri
  lazy val metrics = new IdApiMetrics(Config.stage)

  implicit class FutureWSLike(f: Future[WSResponse]) {
    /**
     * Example of ID API Response: `{"status":"error","errors":[{"message":"Forbidden","description":"Field access denied","context":"privateFields.billingAddress1"}]}`
     * @return
     */
    private def applyOnSpecificErrors(reasons: List[String])(block: WSResponse => Unit): Unit = {
      def errorMessageFilter(error: JsValue): Boolean =
        (error \ "message").asOpt[JsString].exists(e => reasons.contains(e.value))

      f.map(response =>
        (response.json \ "status").asOpt[String].filter(_ == "error")
          .foreach(_ => (response.json \ "errors").asOpt[JsArray].flatMap(_.value.headOption)
            .filter(errorMessageFilter)
            .foreach(_ => block(response))))
    }

    def withWSFailureLogging(request: WSRequest) = {
      f.onFailure {
        case e: Throwable =>
          logger.error("Connection error: " + request.url, e)
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

    def withCloudwatchMonitoringOfPut = withCloudwatchMonitoring("PUT")
  }

  private def authoriseCall = (request: WSRequest) =>
    request.withHeaders(("X-GU-ID-Client-Access-Token", s"Bearer ${Config.Identity.apiToken}"))

  def userLookupByEmail: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user"))

    email => endpoint.withQueryString(("emailAddress", email)).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  def userLookupByScGuUCookie: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/")))

    cookieValue => endpoint.withHeaders(("X-GU-ID-FOWARDED-SC-GU-U", cookieValue)).execute()
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfGet
  }

  def createGuest: JsValue => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest"))

    guestJson => endpoint.post(guestJson)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPost
  }

  override def convertGuest: (JsValue, IdentityToken) => Future[WSResponse] = (json, token) => {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest/password"))

    endpoint
      .withHeaders("X-Guest-Registration-Token" -> token.toString)
      .put(json)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPut
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