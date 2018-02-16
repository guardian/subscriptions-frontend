package actions

import configuration.Config
import controllers.routes
import com.gu.googleauth
import com.gu.googleauth.{AuthAction, GoogleAuthConfig}
import play.api.libs.ws.WSClient
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

final class OAuthActions(override val wsClient: WSClient, commonActions: CommonActions, bodyParser: BodyParser[AnyContent], val authConfig: GoogleAuthConfig) extends googleauth.LoginSupport with googleauth.Filters {

  import commonActions._

  def loginAction()(implicit request: RequestHeader) = startGoogleLogin()

  val loginTarget = routes.OAuth.loginAction()
  lazy val groupChecker = Config.googleGroupChecker
  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest, AnyContent] = new AuthAction(authConfig, loginTarget, bodyParser)

  override val failureRedirectTarget: Call = routes.Homepage.index()
  override val defaultRedirectTarget: Call = routes.OAuth.loginAction()

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val AuthorisedTester = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "directteam@guardian.co.uk",
    "subscriptions.dev@guardian.co.uk",
    "membership.dev@guardian.co.uk",
    "membership.wildebeest@guardian.co.uk",
    "memsubs.dev@guardian.co.uk",
    "identitydev@guardian.co.uk",
    "touchpoint@guardian.co.uk",
    "crm@guardian.co.uk",
    "dig.qa@guardian.co.uk",
    "membership.testusers@guardian.co.uk"
  ))

  val StaffAuthorisedForCASAction = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "customer.operations@guardian.co.uk",
    "directteam@guardian.co.uk",
    "userhelp@guardian.co.uk",
    "dig.qa@guardian.co.uk",
    "subscriptions.dev@guardian.co.uk",
    "subscriptions.cas@guardian.co.uk",
    "ios@guardian.co.uk"
  ))
}
