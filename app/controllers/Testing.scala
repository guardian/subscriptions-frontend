package controllers

import actions.CommonActions._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{Controller, Cookie}
import utils.TestUsers.testUsers

object Testing extends Controller with LazyLogging {

  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  val PreSigninTestCookieName = "pre-signin-test-user"

  def testUser = GoogleAuthenticatedStaffAction { implicit request =>
    val headers = request.headers
    logger.info(s"remoteAddress=${request.remoteAddress} $X_FORWARDED_FOR=${headers.getAll(X_FORWARDED_FOR).mkString("_")} $FORWARDED=${headers.getAll(FORWARDED).mkString("_")}")

    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString")
    val testUserCookie = new Cookie(PreSigninTestCookieName, testUserString, Some(30 * 60), httpOnly = true)
    Ok(views.html.testing.testUsers(testUserString)).withCookies(testUserCookie, analyticsOffCookie)
  }

  def analyticsOff = CachedAction {
    Ok(s"${analyticsOffCookie.name} cookie dropped").withCookies(analyticsOffCookie)
  }
}
