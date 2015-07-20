package actions

import com.gu.identity.play.IdUser
import com.typesafe.scalalogging.LazyLogging
import configuration.QA.passthroughCookie
import configuration.Config.Identity.testUserCookieName
import controllers.{Cached, NoCache}
import play.api.mvc._
import services.{AuthCookie, IdentityService, CheckoutService, TouchpointBackend}


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CommonActions {

  val NoCacheAction = resultModifier(noCache)

  val GoogleAuthAction = OAuthActions.AuthAction

  val GoogleAuthenticatedStaffAction: ActionBuilder[RequestWithServices] =
    NoCacheAction andThen PreReleaseFeature andThen WrapServices

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

  class RequestWithCookies[A](request: Request[A]) extends WrappedRequest[A](request) {
    val testUserCookie = request.cookies.get(testUserCookieName)
    val qaCookie = request.cookies.get(passthroughCookie.name)
    val identityCookie = request.cookies.get("SC_GU_U")
  }

  class RequestWithServices[A](request: Request[A]) extends RequestWithCookies[A](request) {
    private val touchpointBackend =
      testUserCookie.fold(TouchpointBackend.Normal) { cookie =>
        TouchpointBackend.forUser(cookie.value)
      }

    val zuoraService = touchpointBackend.zuoraService
    val salesforceService = touchpointBackend.salesforceService
    val checkoutService = new CheckoutService(IdentityService, salesforceService, zuoraService)

    val identityUser: Future[Option[IdUser]] =
      identityCookie.fold(Future.successful(None: Option[IdUser])) { cookie =>
        IdentityService.userLookupByScGuU(AuthCookie(cookie.value))
      }
  }

  object WrapServices extends ActionBuilder[RequestWithServices] {
    override def invokeBlock[A](r: Request[A], f: (RequestWithServices[A]) => Future[Result]) =
      f(new RequestWithServices[A](r))
  }

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }
}
