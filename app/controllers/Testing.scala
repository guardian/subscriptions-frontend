package controllers

import actions.CommonActions._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{Controller, Cookie}
import utils.TestUsers.testUsers

object Testing extends Controller with LazyLogging {

  val UnauthenticatedTestUserCookieName = "subscriptions-test-user-name"

  def testUser = GoogleAuthenticatedStaffAction { implicit request =>

    val testUserString = testUsers.generate()

    logger.info(s"Generated test user string $testUserString")

    val testUserCookie = new Cookie(UnauthenticatedTestUserCookieName, testUserString, Some(30 * 60), httpOnly = true)
    Ok(views.html.testing.testUsers(testUserString)).withCookies(testUserCookie)
  }
}
