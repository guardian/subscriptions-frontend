package actions

import actions.OAuthActions._
import com.gu.googleauth
import com.typesafe.scalalogging.LazyLogging
import configuration.QA.passthroughCookie
import controllers.{Cached, NoCache}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CommonActions {

  val NoCacheAction = resultModifier(noCache)

  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = AuthAction

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val AuthorisedTester = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "directteam@guardian.co.uk",
    "subscriptions.dev@guardian.co.uk",
    "memsubs.dev@guardian.co.uk",
    "touchpoint@guardian.co.uk",
    "crm@guardian.co.uk"
  ))

  val StaffAuthorisedForCASAction = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "customer.experience@guardian.co.uk",
    "directteam@guardian.co.uk",
    "userhelp@guardian.co.uk",
    "subscriptions.dev@guardian.co.uk"
  ))

  val CachedAction = resultModifier(Cached(_))

  def noCache(result: Result): Result = NoCache(result)

  object PreReleaseFeature extends ActionBuilder[Request] with LazyLogging {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      val qaCookie = request.cookies.get(passthroughCookie.name)
      if (qaCookie.exists(_.value == passthroughCookie.value)) {
        block(request)
      } else {
        qaCookie.foreach(_ => logger.warn("Invalid QA Cookie supplied"))
        OAuthActions.AuthAction.authenticate(request, block)
      }
    }
  }

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }
}
