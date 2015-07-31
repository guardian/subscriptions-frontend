package utils

import com.github.nscala_time.time.Imports._
import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config
import play.api.mvc.RequestHeader
import services.AuthenticationService.authenticatedUserFor

object TestUsers {

  val ValidityPeriod = 2.days

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.Identity.testUsersSecret),
    recency = ValidityPeriod.standardDuration
  )

  private def isTestUser(username: String): Boolean = TestUsers.testUsers.isValid(username)

  def isTestUser(alternateSource: => Option[String] = None)(implicit request: RequestHeader): Boolean =
    authenticatedUserFor(request).fold(alternateSource)(_.user.displayName.flatMap(_.split(' ').headOption)).exists(isTestUser)
}
