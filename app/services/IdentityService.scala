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
  import IdentityService._

  def doesUserExist(email: String): Future[Boolean] =
    identityApiClient.userLookupByEmail(email).map { response =>
      (response.json \ "user" \ "id").asOpt[String].isDefined
    }

  def userLookupByScGuU(authCookie: AuthCookie): Future[Option[IdUser]] =
    identityApiClient.userLookupByScGuUCookie(authCookie.value).map { response =>
      (response.json \ "user").asOpt[IdUser]
    }

 def registerGuest(personalData: PersonalData): Future[GuestUser] = {
    val json = personalData.convertToUser()
    identityApiClient.createGuest(json).map(response => response.json.as[GuestUser])
  }

  def convertGuest(password: String, token: IdentityToken): Future[Unit] = {
    val json = Json.obj("password" -> password)
    identityApiClient.convertGuest(json, token).map { response =>
      if (response.status != Status.OK) {
        throw new IdentityGuestPasswordError(response.body)
      }
    }
  }

  def updateUserDetails(personalData: PersonalData, userId: UserId, authCookie: AuthCookie): Future[Unit] = {
    val updatedFields =
      createOnlyFields.foldLeft(personalData.convertToUser()) { (map, field) => map - field }

    identityApiClient.updateUserDetails(updatedFields, userId, authCookie).map(_ => Unit)
  }
}

object IdentityService extends IdentityService(IdentityApiClient) {
  val primaryEmailAddress = "primaryEmailAddress"
  val publicFields = "publicFields"
  val createOnlyFields = Seq(primaryEmailAddress, publicFields)

  class IdentityGuestPasswordError(jsonMsg: String) extends RuntimeException(s"Cannot set password for Identity guest user. Json response form service: $jsonMsg")

  implicit class PersonalDataJsonSerialiser(personalData: PersonalData) {
    private def convertToPrivateFields(): JsObject = Json.obj(
      "firstName" -> personalData.firstName,
      "secondName" -> personalData.lastName,
      "billingAddress1" -> personalData.address.address1,
      "billingAddress2" -> personalData.address.address2,
      "billingAddress3" -> personalData.address.town,
      "billingPostcode" -> personalData.address.postcode,
      "billingCountry" -> "United Kingdom"
    )

    def convertToUser(): JsObject = {
      Json.obj(
        primaryEmailAddress -> personalData.email,
        publicFields -> Json.obj(
          "displayName" -> s"${personalData.firstName} ${personalData.lastName}"
        ),
        "privateFields" -> convertToPrivateFields(),
        "statusFields" ->
          Json.obj("receiveGnmMarketing" -> personalData.receiveGnmMarketing))
    }
  }
}

trait IdentityApiClient {

  def userLookupByScGuUCookie: String => Future[WSResponse]

  def createGuest: JsValue => Future[WSResponse]

  def convertGuest: (JsValue, IdentityToken) => Future[WSResponse]

  def userLookupByEmail: String => Future[WSResponse]

  def updateUserDetails: (JsValue, UserId, AuthCookie) => Future[WSResponse]
}

object IdentityApiClient extends IdentityApiClient with LazyLogging {

  lazy val identityEndpoint = Config.Identity.baseUri
  lazy val metrics = new IdApiMetrics(Config.stage)

  implicit class FutureWSLike(eventualResponse: Future[WSResponse]) {
    /**
     * Example of ID API Response: `{"status":"error","errors":[{"message":"Forbidden","description":"Field access denied","context":"privateFields.billingAddress1"}]}`
     * @return
     */
    private def applyOnSpecificErrors(reasons: List[String])(block: WSResponse => Unit): Unit = {
      def errorMessageFilter(error: JsValue): Boolean =
        (error \ "message").asOpt[JsString].exists(e => reasons.contains(e.value))

      eventualResponse.map(response =>
        (response.json \ "status").asOpt[String].filter(_ == "error")
          .foreach(_ => (response.json \ "errors").asOpt[JsArray].flatMap(_.value.headOption)
            .filter(errorMessageFilter)
            .foreach(_ => block(response))))
    }

    def withWSFailureLogging(request: WSRequest) = {
      eventualResponse.onFailure {
        case e: Throwable =>
          logger.error("Connection error: " + request.url, e)
      }
      applyOnSpecificErrors(List("Access Denied", "Forbidden")) { r =>
        logger.error(s"Call to '${request.url}' completed with the following error: ${r.body}")
      }
      eventualResponse
    }

    def withCloudwatchMonitoring(method: String) = {
      eventualResponse.map(response => metrics.recordResponse(response.status, method))
      applyOnSpecificErrors(List("Access Denied", "Forbidden"))(_ => metrics.recordAuthenticationError)
      eventualResponse
    }

    def withCloudwatchMonitoringOfPost = withCloudwatchMonitoring("POST")

    def withCloudwatchMonitoringOfGet = withCloudwatchMonitoring("GET")

    def withCloudwatchMonitoringOfPut = withCloudwatchMonitoring("PUT")
  }

  private val authoriseCall = (request: WSRequest) =>
    request.withHeaders(("X-GU-ID-Client-Access-Token", s"Bearer ${Config.Identity.apiToken}"))

  override val userLookupByEmail: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user"))

    email => endpoint.withQueryString(("emailAddress", email)).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  override val userLookupByScGuUCookie: String => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/")))

    cookieValue => endpoint.withHeaders(("X-GU-ID-FOWARDED-SC-GU-U", cookieValue)).execute()
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfGet
  }

  override val createGuest: JsValue => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest"))

    guestJson => endpoint.post(guestJson)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPost
  }

  override val convertGuest: (JsValue, IdentityToken) => Future[WSResponse] = (json, token) => {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest/password"))

    endpoint
      .withHeaders("X-Guest-Registration-Token" -> token.toString)
      .put(json)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPut
  }

  override val updateUserDetails: (JsValue, UserId, AuthCookie) => Future[WSResponse] = { (userJson, userId, authCookie) =>
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user/${userId.id}"))

    endpoint.withHeaders(("X-GU-ID-FOWARDED-SC-GU-U", authCookie.value)).post(userJson)
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
