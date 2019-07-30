package utils

import java.time.Duration.ofDays

import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config
import controllers.Testing
import model.SubscriptionData
import play.api.mvc.{Cookies, RequestHeader}
import services.{AuthenticationService, IdMinimalUser}

object TestUsers {

  val ValidityPeriod = ofDays(2)

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.Identity.testUsersSecret),
    recency = ValidityPeriod
  )

  private def isTestUser(username: String): Boolean = TestUsers.testUsers.isValid(username)

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

class TestUsers(authenticationService: AuthenticationService) {

  import TestUsers._

  def isTestUser[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C)(implicit request: RequestHeader)
  : Option[TestUserCredentialType[_]] = {

    authenticationService.authenticatedUserFor(request).map(_.user).fold[Option[TestUserCredentialType[_]]] {
      permittedAltCredentialType.passes(altCredentialSource)
    }(SignedInUsername.passes)
  }
}
