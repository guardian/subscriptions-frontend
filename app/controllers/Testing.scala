package controllers

import javax.inject.Inject

import actions.CommonActions._
import actions.OAuthActions
import com.typesafe.scalalogging.LazyLogging
import model.Subscriptions.{SubscriptionOption, SubscriptionProduct}
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Cookie}
import services.TouchpointBackend
import utils.TestUsers.testUsers

class Testing @Inject()(override val wsClient: WSClient)  extends Controller with LazyLogging with OAuthActions {

  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  val PreSigninTestCookieName = "pre-signin-test-user"

  lazy val products = TouchpointBackend.Test.catalogService.unsafeCatalog.allSubs

  def testUser = AuthorisedTester { implicit request =>
    val headers = request.headers
    logger.info(s"remoteAddress=${request.remoteAddress} $X_FORWARDED_FOR=${headers.getAll(X_FORWARDED_FOR).mkString("_")} $FORWARDED=${headers.getAll(FORWARDED).mkString("_")}")

    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString")
    val testUserCookie = Cookie(PreSigninTestCookieName, testUserString, Some(30 * 60), httpOnly = true)
    Ok(views.html.testing.testUsers(testUserString, products)).withCookies(testUserCookie, analyticsOffCookie)
  }

  def analyticsOff = CachedAction {
    Ok(s"${analyticsOffCookie.name} cookie dropped").withCookies(analyticsOffCookie)
  }

}
