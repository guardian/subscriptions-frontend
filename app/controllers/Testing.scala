package controllers

import javax.inject.Inject

import actions.{CommonActions, OAuthActions}
import com.gu.memsub.subsv2.CatalogPlan.Paid
import com.typesafe.scalalogging.LazyLogging
import model.Subscriptions.{SubscriptionOption, SubscriptionProduct}
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Cookie}
import services.{TouchpointBackend, TouchpointBackends}
import utils.TestUsers.testUsers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Testing {
  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  val PreSigninTestCookieName = "pre-signin-test-user"
}

class Testing (touchpointBackend: TouchpointBackend, commonActions: CommonActions, oAuthActions: OAuthActions)  extends Controller with LazyLogging {

  import commonActions._
  import oAuthActions._

  lazy val products: Future[List[List[Paid]]] = touchpointBackend.catalogService.catalog.map(_.valueOr(e => throw new IllegalStateException(s"$e while getting catalog")).allSubs)

  def testUser = AuthorisedTester.async { implicit request =>
    val headers = request.headers
    logger.info(s"remoteAddress=${request.remoteAddress} $X_FORWARDED_FOR=${headers.getAll(X_FORWARDED_FOR).mkString("_")} $FORWARDED=${headers.getAll(FORWARDED).mkString("_")}")

    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString")
    val testUserCookie = Cookie(Testing.PreSigninTestCookieName, testUserString, Some(30 * 60), httpOnly = true)
    products.map { products =>
      Ok(views.html.testing.testUsers(testUserString, products)).withCookies(testUserCookie, Testing.analyticsOffCookie)
    }
  }

  def analyticsOff = CachedAction {
    Ok(s"${Testing.analyticsOffCookie.name} cookie dropped").withCookies(Testing.analyticsOffCookie)
  }

}
