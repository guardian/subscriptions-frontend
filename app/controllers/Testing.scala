package controllers

import actions.{CommonActions, OAuthActions}
import com.gu.memsub.subsv2.CatalogPlan.Paid
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{BaseController, ControllerComponents, Cookie}
import services.TouchpointBackend
import utils.TestUsers.testUsers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Testing {
  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  val PreSigninTestCookieName = "pre-signin-test-user"
}

class Testing (
  touchpointBackend: TouchpointBackend,
  commonActions: CommonActions,
  oAuthActions: OAuthActions,
  override protected val controllerComponents: ControllerComponents
) extends BaseController with LazyLogging {

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
