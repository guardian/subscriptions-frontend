package utils

import com.github.nscala_time.time.Imports._
import com.gu.identity.play.IdMinimalUser
import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config

object TestUsers {

  val ValidityPeriod = 10.minutes

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.Identity.testUsersSecret),
    recency = ValidityPeriod.standardDuration
  )

  def isTestUser(user: IdMinimalUser): Boolean =
    user.displayName.flatMap(_.split(' ').headOption).exists(TestUsers.testUsers.isValid)

  def isTestUser(username: String): Boolean =
    TestUsers.testUsers.isValid(username)
}
