package services

import com.amazonaws.regions.{Region, Regions}
import com.gu.identity.play.{AccessCredentials, CookieBuilder, AuthenticatedIdUser, IdUser}
import com.gu.monitoring.{AuthenticationMetrics, CloudWatch, RequestMetrics, StatusMetrics}
import com.gu.memsub.NormalisedTelephoneNumber
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.{SubsError, PersonalData}
import play.api.Play.current
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequest, WSResponse}
import play.api.mvc.Cookie

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.concurrent.Future

class IdentityService(identityApiClient: => IdentityApiClient) extends LazyLogging {

  import IdentityService._

  def doesUserExist(email: String): Future[Boolean] =
    identityApiClient.userLookupByEmail(email).map { response =>
      (response.json \ "user" \ "id").asOpt[String].isDefined
    }

  def userLookupByCredentials(accessCredentials: AccessCredentials): Future[Option[IdUser]] = accessCredentials match {
    case cookies: AccessCredentials.Cookies => identityApiClient.userLookupByCookies (cookies).map {
        response => (response.json \ "user").asOpt[IdUser]
      }
    case _ => Future.successful {
      logger.error("ID API only supports forwarded access for Cookies")
      None
    }
  }

  def registerGuest(personalData: PersonalData): Future[Either[Seq[SubsError], IdentitySuccess]] = {
    identityApiClient.createGuest(personalData).map { response =>
      response.json.asOpt[GuestUser] match {
        case Some(guest) => Right(IdentitySuccess(guest))
        case None => Left(Seq(
          IdentityFailure(
          "Guest user could not be registered as Identity user",
          personalData.toString,
          Some(response.json.toString()))))
      }
    }
  }

  def convertGuest(password: String, token: IdentityToken): Future[Seq[Cookie]] = {
    IdentityApiClient.convertGuest(password, token).map { r =>
      if (r.status == Status.OK) {
        CookieBuilder.fromGuestConversion(r.json, Some(Config.Identity.sessionDomain)).fold({ err =>
          logger.error(s"Error while parsing the identity cookies: $err")
          Seq.empty // Worst case the user is not automatically logged in
        }, identity)
      } else {
        throw new IdentityGuestPasswordError(r.body)
      }
    }
  }

  def updateUserDetails(personalData: PersonalData)(authenticatedUser: AuthenticatedIdUser): Future[Unit] =
    authenticatedUser.credentials match {
      case cookies: AccessCredentials.Cookies =>
        identityApiClient.updateUserDetails(
          personalData,
          cookies
        ).map(_ => Unit)
      case _ => Future.successful(logger.error("ID API only supports forwarded access for Cookies"))
    }
}

object IdentityService extends IdentityService(IdentityApiClient) {

  sealed trait IdentityResult
  case class IdentitySuccess(userData: UserIdData) extends IdentityResult
  case class IdentityFailure(msg: String,
                             requestData: String,
                             errorResponse: Some[String]) extends SubsError with IdentityResult {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  class IdentityGuestPasswordError(jsonMsg: String) extends RuntimeException(s"Cannot set password for Identity guest user.")

}

object PersonalDataJsonSerialiser {
  val primaryEmailAddress = "primaryEmailAddress"
  val publicFields = "publicFields"

  implicit def convertToUser(personalData: PersonalData): JsObject = {
    val telephoneNumber = NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber,personalData.address.country)
    Json.obj(
      primaryEmailAddress -> personalData.email,
      publicFields -> Json.obj(
        "displayName" -> s"${personalData.first} ${personalData.last}"
      ),
      "privateFields" -> Json.obj(
        "firstName" -> personalData.first,
        "secondName" -> personalData.last,
        "billingAddress1" -> personalData.address.lineOne,
        "billingAddress2" -> personalData.address.lineTwo,
        "billingAddress3" -> personalData.address.town,
        "billingAddress4" -> personalData.address.countyOrState,
        "billingPostcode" -> personalData.address.postCode,
        "billingCountry"  -> personalData.country.name
      ).++(
         telephoneNumber.fold[JsObject](Json.obj()){t =>
           Json.obj(
             "telephoneNumber" -> Json.toJson(t)
           )}
    ),
      "statusFields" ->
        Json.obj("receiveGnmMarketing" -> personalData.receiveGnmMarketing))
  }
}

trait IdentityApiClient {

  import PersonalDataJsonSerialiser._

  val createOnlyFields = Seq(primaryEmailAddress, publicFields)
  type Password = String
  type Email = String

  def userLookupByCookies: AccessCredentials.Cookies => Future[WSResponse]

  def createGuest: PersonalData => Future[WSResponse]

  def convertGuest: (Password, IdentityToken) => Future[WSResponse]

  def userLookupByEmail: Email => Future[WSResponse]

  def updateUserDetails: (PersonalData, AccessCredentials.Cookies) => Future[WSResponse]
}

object IdentityApiClient extends IdentityApiClient with LazyLogging {

  import PersonalDataJsonSerialiser._

  lazy val identityEndpoint = Config.Identity.baseUri
  lazy val metrics = new IdApiMetrics(Config.stage)

  implicit class FutureWSLike(eventualResponse: Future[WSResponse]) {
    /**
     * Example of ID API Response: `{"status":"error","errors":[{"message":"Forbidden","description":"Field access denied","context":"privateFields.billingAddress1"}]}`
      *
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

  override val userLookupByEmail: Email => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user"))

    email => endpoint.withQueryString(("emailAddress", email)).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  override val userLookupByCookies: AccessCredentials.Cookies => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/")))

    cookies => endpoint.withHeaders(cookies.forwardingHeader).execute()
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfGet
  }

  override val createGuest: PersonalData => Future[WSResponse] = {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest"))

    guestData => {
      endpoint.post(guestData: JsObject)
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfPost
    }
  }

  override val convertGuest: (Password, IdentityToken) => Future[WSResponse] = (password, token) => {
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/guest/password"))
    val json = Json.obj("password" -> password)

    endpoint
      .withHeaders("X-Guest-Registration-Token" -> token.toString)
      .put(json)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPut
  }

  override val updateUserDetails: (PersonalData, AccessCredentials.Cookies) => Future[WSResponse] = { (personalData, authCookies) =>
    val endpoint = authoriseCall(WS.url(s"$identityEndpoint/user/me"))
    val userJson: JsObject = personalData
    val updatedFields =
      createOnlyFields.foldLeft(userJson) { (map, field) => map - field }

    endpoint.withHeaders(authCookies.forwardingHeader).post(updatedFields)
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
