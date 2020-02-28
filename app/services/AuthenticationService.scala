package services

import java.util.concurrent.Executors

import com.gu.identity.auth.UserCredentials
import com.gu.identity.model.User
import com.gu.identity.play.IdentityPlayAuthService
import com.gu.identity.play.IdentityPlayAuthService.UserCredentialsMissingError
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import org.http4s.Uri
import play.api.mvc.{Cookie, RequestHeader}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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

// Class used to authenticate a user. Authentication is done by calling back to identity API.
// This is so session invalidation can be taken into account when authenticating a user.
// See comments on IdentityPlayAuthService for more information.
class AuthenticationService private (identityPlayAuthService: IdentityPlayAuthService) {

  import AuthenticationService._

  // Even though a request to identity API is made when this method is called,
  // the IO instance returned by getUserFromRequest() is executed synchronously,
  // as changing the result type to Future resulted in a PR with a very complicated diff:
  // https://github.com/guardian/subscriptions-frontend/pull/1265
  //
  // It was deemed that the risk of a bug being introduced in #1265
  // was greater than the risk of a user getting a slower than normal response
  // (as a result of all threads available to execute this method call being blocked).
  // The latter was considered low risk, since the subscriptions-frontend app is low traffic
  // and a dedicated thread pool of 30 threads has been reserved
  // to execute these blocking calls (c.f. Authentication.unsafeInit()).
  // The fact that subscriptions-frontend is a deprecated application also factored into the decision.
  def authenticatedUserFor(request: RequestHeader): Option[AuthenticatedIdUser] = {
    identityPlayAuthService.getUserFromRequest(request)
      .map { case (credentials, user) => buildAuthenticatedUser(credentials, user) }
      .attempt
      .unsafeRunTimed(limit = 5.seconds) // fairly arbitrary
      .flatMap {
        case Left(err) =>
          logUserAuthenticationError(err)
          None
        case Right(user) => Some(user)
      }
  }
}

object AuthenticationService {

  def unsafeInit(identityApiEndpoint: String, accessToken: String): AuthenticationService = {
    // See comment above for why dedicated blocking execution context is used.
    implicit val blockingExecutionContext: ExecutionContext =
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))
    val identityApiUri = Uri.unsafeFromString(identityApiEndpoint)
    // Target client not relevant; only applicable to crypto access tokens,
    // which aren't used to authenticate requests for this application
    // (crypto access tokens only used to authenticate requests from native apps).
    val identityPlayAuthService = IdentityPlayAuthService.unsafeInit(identityApiUri, accessToken, targetClient = None)
    new AuthenticationService(identityPlayAuthService)
  }

  def buildAuthenticatedUser(credentials: UserCredentials, user: User): AuthenticatedIdUser = {
    val accessCredentials = credentials match {
      case UserCredentials.SCGUUCookie(value) => AccessCredentials.Cookies(scGuU = value)
      case UserCredentials.CryptoAccessToken(value, _) => AccessCredentials.Token(tokenText = value)
    }
    AuthenticatedIdUser(accessCredentials, IdMinimalUser(user.id, user.publicFields.username))
  }

  // Logs failure to authenticate a user.
  // User shouldn't necessarily be signed in,
  // in which case, don't log failure to authenticate as an error.
  // All other failures are considered errors.
  def logUserAuthenticationError(error: Throwable): Unit =
    error match {
      // Utilises the custom error introduced in: https://github.com/guardian/identity/pull/1578
      case _: UserCredentialsMissingError => SafeLogger.info(s"unable to authorize user - $error")
      case _ => SafeLogger.error(scrub"unable to authorize user", error)
    }
}
