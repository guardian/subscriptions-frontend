package services

import com.amazonaws.regions.{Region, Regions}
import com.gu.identity.play._
import com.gu.monitoring.{AuthenticationMetrics, CloudWatch, RequestMetrics, StatusMetrics}
import com.gu.memsub.{Address, NormalisedTelephoneNumber}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import model.error.SubsError
import play.api.Play.current
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Cookie

import scala.language.{higherKinds, implicitConversions}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Monad, NonEmptyList, \/}
import model.error.IdentityService._
import services.IdentityApiClient.{authoriseCall, identityEndpoint}
import services.PersonalDataJsonSerialiser.convertToUser

class IdentityService[M[_]](identityApiClient: => IdentityApiClient[M])(implicit monad: Monad[M]) extends LazyLogging {

  import IdentityService._

  def doesUserExist(email: String): M[Boolean] =
    monad.map(identityApiClient.userLookupByEmail(email)) { response =>
      response.status match {
        case Status.OK => true
        case Status.NOT_FOUND => false
        case status => {
          logger.error(s"ID API failed on email check with HTTP status $status")
          false
        }
      }

    }

  def userLookupByCredentials(accessCredentials: AccessCredentials): M[Option[IdUser]] = accessCredentials match {
    case cookies: AccessCredentials.Cookies => monad.map(identityApiClient.userLookupByCookies(cookies)) {
      response => (response.json \ "user").asOpt[IdUser]
    }
    case _ => monad.point {
      logger.error("ID API only supports forwarded access for Cookies")
      None
    }
  }

  def registerGuest(personalData: PersonalData, delivery: Option[Address]): M[NonEmptyList[SubsError] \/ IdentitySuccess] = {
    monad.map(identityApiClient.createGuest(personalData, delivery)) { response =>
      response.json.asOpt[GuestUser] match {
        case Some(guest) => \/.right(IdentitySuccess(guest))
        case None => \/.left(NonEmptyList(IdentityFailure(
          "Guest user could not be registered as Identity user",
          Some(personalData.toStringSanitized),
          Some(response.json.toString()))))
      }
    }
  }

  def consentEmail(email: String): M[\/[NonEmptyList[IdentityFailure], Unit]] = monad.map(identityApiClient.consentEmail(email)) {
    response =>
      if (response.status == 200)
        \/.right(Unit)
      else
        \/.left(NonEmptyList(IdentityFailure(
          "Consent email could not be sent for user",
          Some(email),
          Some(response.json.toString()))))
  }

  def convertGuest(password: String, token: IdentityToken, marketingOptIn: Boolean): M[Seq[Cookie]] = {
    monad.map(identityApiClient.convertGuest(password, token, marketingOptIn)) { r =>
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

  def updateUserDetails(personalData: PersonalData, delivery: Option[Address])(authenticatedUser: AuthenticatedIdUser): M[NonEmptyList[SubsError] \/ IdentitySuccess] =
    authenticatedUser.credentials match {
      case cookies: AccessCredentials.Cookies =>
        monad.map(identityApiClient.updateUserDetails(
          personalData,
          delivery,
          cookies
        )) { response =>
          response.status match {
            case Status.OK => \/.right(IdentitySuccess(RegisteredUser(IdMinimalUser("", None))))
            case _ =>
              \/.left(NonEmptyList(IdentityFailure(
                "Registered user's details could not be updated in Identity",
                Some(personalData.toStringSanitized),
                Some(response.json.toString()))))
          }
        }

      case _ => monad.point(\/.left(NonEmptyList(IdentityFailure(
        "ID API only supports forwarded access for Cookies",
        Some(personalData.toStringSanitized)))))
    }
}

object IdentityService {

  class IdentityGuestPasswordError(jsonMsg: String) extends RuntimeException(s"Cannot set password for Identity guest user.")

}

object PersonalDataJsonSerialiser {
  val primaryEmailAddress = "primaryEmailAddress"
  val publicFields = "publicFields"

  implicit def convertToUser(personalData: PersonalData, deliveryAddress: Option[Address]): JsObject = {
    val telephoneNumber = NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber, personalData.address.country)
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
        "billingCountry" -> personalData.address.country.fold(personalData.address.countryName)(_.name)
      ).++(
        personalData.title.fold(Json.obj())(title =>
          Json.obj("title" -> title.title))
      ).++(
        telephoneNumber.fold[JsObject](Json.obj()) { t =>
          Json.obj(
            "telephoneNumber" -> Json.toJson(t)
          )
        }
      ).++(deliveryAddress.fold(Json.obj()) { addr =>
        Json.obj(
          "address1" -> addr.lineOne,
          "address2" -> addr.lineTwo,
          "address3" -> addr.town,
          "address4" -> addr.countyOrState,
          "postcode" -> addr.postCode,
          "country" -> addr.country.fold(addr.countryName)(_.name)
        )
      }),
      "statusFields" ->
        Json.obj("receiveGnmMarketing" -> personalData.receiveGnmMarketing))
  }
}

trait IdentityApiClient[M[_]] {

  import PersonalDataJsonSerialiser._

  val createOnlyFields = Seq(primaryEmailAddress, publicFields)
  type Password = String
  type Email = String

  def userLookupByCookies: AccessCredentials.Cookies => M[WSResponse]

  def createGuest: (PersonalData, Option[Address]) => M[WSResponse]

  def convertGuest: (Password, IdentityToken, Boolean) => M[WSResponse]

  def userLookupByEmail: Email => M[WSResponse]

  def updateUserDetails: (PersonalData, Option[Address], AccessCredentials.Cookies) => M[WSResponse]

  def consentEmail: Email => M[WSResponse]
}

class IdentityApiClientImpl(wsClient: WSClient)(implicit executionContext: ExecutionContext) extends IdentityApiClient[Future] with LazyLogging {

  import IdentityApiClient._

  override val userLookupByEmail: Email => Future[WSResponse] = {
    val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/user"))

    email =>
      endpoint.withQueryString(("emailAddress", email)).execute()
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfGet
  }

  override val userLookupByCookies: AccessCredentials.Cookies => Future[WSResponse] = {
    val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/user/me").withHeaders(("Referer", s"$identityEndpoint/")))

    cookies =>
      endpoint.withHeaders(cookies.forwardingHeader).execute()
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfGet
  }

  override val createGuest: (PersonalData, Option[Address]) => Future[WSResponse] = {
    case (data, addr) =>
      val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/guest"))
      endpoint.post(convertToUser(data, addr))
        .withWSFailureLogging(endpoint)
        .withCloudwatchMonitoringOfPost
  }

  override val convertGuest: (Password, IdentityToken, Boolean) => Future[WSResponse] = (password, token, marketingOptIn) => {
    //If marketingOptIn is true then we will already have sent an email to the user asking them
    //to confirm their marketing preference. In this case to prevent sending them 2 emails in quick
    //succession we suppress the standard validation email with the validate-email query parameter.
    //more info here: https://docs.google.com/document/d/1JDEpehzToi9aAMg4Fk7n_mnvQ_GOOf_kgOPSyWLBweA
    val validateEmail = if (marketingOptIn) 0 else 1 //Identity expects 1/0 rather than true/false
    val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/guest/password?validate-email=$validateEmail"))
    val json = Json.obj("password" -> password)

    endpoint
      .withHeaders("X-Guest-Registration-Token" -> token.toString)
      .put(json)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPut
  }

  override val updateUserDetails: (PersonalData, Option[Address], AccessCredentials.Cookies) => Future[WSResponse] = { (personalData, addr, authCookies) =>
    val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/user/me"))
    val userJson: JsObject = convertToUser(personalData, addr)
    val updatedFields =
      createOnlyFields.foldLeft(userJson) { (map, field) => map - field }

    endpoint.withHeaders(authCookies.forwardingHeader).post(updatedFields)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPost
  }

  override val consentEmail: Email => Future[WSResponse] = { email =>
    val endpoint = authoriseCall(wsClient.url(s"$identityEndpoint/consent-email"))

    val json = Json.obj("email" -> email, "set-consents" -> List("supporter"))
    endpoint.post(json)
      .withWSFailureLogging(endpoint)
      .withCloudwatchMonitoringOfPost
  }

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

}

object IdentityApiClient extends LazyLogging {

  import PersonalDataJsonSerialiser._

  lazy val identityEndpoint = Config.Identity.baseUri

  val authoriseCall = (request: WSRequest) =>
    request.withHeaders(("X-GU-ID-Client-Access-Token", s"Bearer ${Config.Identity.apiToken}"))

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
