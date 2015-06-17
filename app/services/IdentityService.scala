package services

import com.amazonaws.regions.{Region, Regions}
import com.gu.identity.play.IdUser
import com.gu.monitoring.{AuthenticationMetrics, CloudWatch, RequestMetrics, StatusMetrics}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GuestUserNotCreated(s : String) extends RuntimeException

class IdentityService(identityApiClient: IdentityApiClient) extends LazyLogging {

  def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] =
    identityApiClient.userLookupByScGuUCookie(cookieValue).map { response =>
      (response.json \ "user").asOpt[IdUser]
    }

  def registerGuest(personalData: PersonalData): Future[Either[GuestUserNotCreated, String]] = {

    val json = JsObject(Map(
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
    ).toSeq)

    for {
      response <- identityApiClient.createGuest(json)

    } yield {
      val jsResult = (response.json \ "guestRegistrationRequest" \ "userId").validate[String]
      if (jsResult.isError) {
        Logger.error(s"Id API response : $jsResult")
        Left(GuestUserNotCreated(s"User not created $jsResult"))
      } else Right(jsResult.get)
    }
  }
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