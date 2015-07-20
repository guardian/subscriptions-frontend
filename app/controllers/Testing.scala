package controllers

import actions.CommonActions._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{Cookie, Controller}
import utils.TestUsers.testUsers
import scala.concurrent.duration._

object Testing extends Controller with LazyLogging {

  //todo - should we filter by email group
  def testUser = GoogleAuthenticatedStaffAction { implicit request =>

    val testUserString = testUsers.generate()

    logger.info(s"Generated test user string $testUserString")
    val testUserCookieConfig: String = "subscriptions-test-user-name"
    val testUserCookie = new Cookie(testUserCookieConfig, testUserString, Some(30 * 60), httpOnly = true)
    Ok(views.html.testing.testUsers(testUserString)).withCookies(testUserCookie)
  }

  //TODO - turn off anayltics

}
