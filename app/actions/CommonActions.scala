package actions

import controllers.{Cached, NoCache}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CommonActions {

  val NoCacheAction = resultModifier(noCache)

  val GoogleAuthAction = OAuthActions.AuthAction

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val CachedAction = resultModifier(Cached(_))

  def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def noCache(result: Result): Result = NoCache(result)
}