package utils

import com.github.nscala_time.time.Imports._
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config
import controllers.Testing
import model.{DigipackData$, SubsFormData}
import play.api.mvc.{Cookies, RequestHeader}
import services.AuthenticationService.authenticatedUserFor

object TestUsers {

  val ValidityPeriod = 2.days

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.Identity.testUsersSecret),
    recency = ValidityPeriod.standardDuration
  )

  private def isTestUser(username: String): Boolean = TestUsers.testUsers.isValid(username)

  sealed trait TestUserCredentialType[C] {
    def token(credential: C): Option[String]
    def passes(credential: C): Option[this.type] = token(credential).filter(isTestUser).map(_ => this)
  }

  object PreSigninTestCookie extends TestUserCredentialType[Cookies] {
    def token(cookies: Cookies) = cookies.get(Testing.PreSigninTestCookieName).map(_.value)
  }

  object NameEnteredInForm extends TestUserCredentialType[SubsFormData] {
    def token(formData: SubsFormData) = Some(formData.personalData.first)
  }

  object SignedInUsername extends TestUserCredentialType[IdMinimalUser] {
    def token(idUser: IdMinimalUser) = idUser.displayName.flatMap(_.split(' ').headOption)
  }


  def isTestUser[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C)(implicit request: RequestHeader)
    : Option[TestUserCredentialType[_]] = {

    authenticatedUserFor(request).map(_.user).fold[Option[TestUserCredentialType[_]]] {
      permittedAltCredentialType.passes(altCredentialSource)
    }(SignedInUsername.passes)
  }
}
