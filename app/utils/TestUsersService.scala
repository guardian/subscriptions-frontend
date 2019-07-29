package utils

import java.time.Duration.ofDays

import com.gu.identity.play.IdMinimalUser
import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config
import controllers.Testing
import model.SubscriptionData
import play.api.mvc.{Cookies, RequestHeader}
import services.AsyncAuthenticationService
import utils.TestUsersService.{SignedInUsername, TestUserCredentialType}

import scala.concurrent.{ExecutionContext, Future}

object TestUsersService {

  val ValidityPeriod = ofDays(2)

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.Identity.testUsersSecret),
    recency = ValidityPeriod
  )

  private def isTestUser(username: String): Boolean = TestUsersService.testUsers.isValid(username)

  sealed trait TestUserCredentialType[C] {
    def token(credential: C): Option[String]
    def passes(credential: C): Option[this.type] = token(credential).filter(isTestUser).map(_ => this)
  }

  object PreSigninTestCookie extends TestUserCredentialType[Cookies] {
    def token(cookies: Cookies) = cookies.get(Testing.PreSigninTestCookieName).map(_.value)
  }

  object NameEnteredInForm extends TestUserCredentialType[Option[SubscriptionData]] {
    def token(formData: Option[SubscriptionData]) = formData.map(_.personalData.first)
  }

  object SignedInUsername extends TestUserCredentialType[IdMinimalUser] {
    def token(idUser: IdMinimalUser) = idUser.displayName.flatMap(_.split(' ').headOption)
  }
}

class TestUsersService(authenticationService: AsyncAuthenticationService)(implicit ec: ExecutionContext) {

  def isTestUser[C](
    permittedAltCredentialsType: TestUserCredentialType[C],
    altCredentialSource: C
  )(implicit request: RequestHeader): Future[Option[TestUserCredentialType[_]]] = {
    authenticationService.tryAuthenticatedUserFor(request).map {
      case None => permittedAltCredentialsType.passes(altCredentialSource)
      case Some(authenticatedIdUser) => SignedInUsername.passes(authenticatedIdUser.user)
    }
  }
}
