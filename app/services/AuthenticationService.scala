package services

import com.gu.identity.auth.UserCredentials
import com.gu.identity.model.User
import com.gu.identity.play.IdentityPlayAuthService
import com.gu.identity.play.IdentityPlayAuthService.UserCredentialsMissingError
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.Config
import org.http4s.Uri
import play.api.mvc.{Cookie, RequestHeader}

import scala.concurrent.ExecutionContext

// The following classes were previously defined in identity-play-auth,
// but have been removed as part of the changes to that library:
// authenticating users via a call to identity API; removing periphery functionality.
// They have been redefined here to reduce diff across PRs,
// but these classes could get refactored / simplified / removed in subsequent PRs.
sealed trait AccessCredentials
object AccessCredentials {
  case class Cookies(scGuU: String, guU: Option[String] = None) extends AccessCredentials {
    val cookies: Seq[Cookie] = Seq(
      Cookie(name = "SC_GU_U", scGuU)
    ) ++ guU.map(c => Cookie(name = "GU_U", c))
  }
  case class Token(tokenText: String) extends AccessCredentials
}
case class IdMinimalUser(id: String, displayName: Option[String])
case class AuthenticatedIdUser(credentials: AccessCredentials, user: IdMinimalUser)

class AuthenticationService(identityPlayAuthService: IdentityPlayAuthService) {

  import AuthenticationService._

  def authenticatedUserFor(request: RequestHeader): Option[AuthenticatedIdUser] = {
    identityPlayAuthService.getUserFromRequest(request)
      .map { case (credentials, user) => buildAuthenticatedUser(credentials, user) }
      .attempt
      .unsafeRunTimed(limit = null)
      .flatMap {
        case Left(err: UserCredentialsMissingError) =>
          SafeLogger.info(s"unable to authenticate user - $err")
          None
        case Left(err) =>
          SafeLogger.error(scrub"unable to authenticate user", err)
          None
        case Right(user) => Some(user)
      }
  }
}

object AuthenticationService {

  def unsafeInit(identityApiEndpoint: String, accessToken: String): AuthenticationService = {
    implicit val ec: ExecutionContext = ???
    val identityApiUri = Uri.unsafeFromString(identityApiEndpoint)
    val identityPlayAuthService = IdentityPlayAuthService.unsafeInit(identityApiUri, accessToken, targetClient = "n/a")
    new AuthenticationService(identityPlayAuthService)
  }

  def buildAuthenticatedUser(credentials: UserCredentials, user: User): AuthenticatedIdUser = {
    val accessCredentials = credentials match {
      case UserCredentials.SCGUUCookie(value) => AccessCredentials.Cookies(scGuU = value)
      case UserCredentials.CryptoAccessToken(value, _) => AccessCredentials.Token(tokenText = value)
    }
    AuthenticatedIdUser(accessCredentials, IdMinimalUser(user.id, user.publicFields.displayName))
  }
}
