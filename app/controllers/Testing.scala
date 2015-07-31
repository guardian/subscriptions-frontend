package controllers

import actions.CommonActions._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Controller
import utils.TestUsers.testUsers

object Testing extends Controller with LazyLogging {

  def testUser = GoogleAuthenticatedStaffAction { implicit request =>

    val testUserString = testUsers.generate()

    logger.info(s"Generated test user string $testUserString")

    Ok(views.html.testing.testUsers(testUserString))
  }
}
