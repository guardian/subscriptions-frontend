package actions

import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CommonActions {

  import CommonActions._
  val noCacheAction = resultModifier(noCache)

  val googleAuthAction = OAuthActions.AuthAction

  val googleAuthenticatedStaffAction = noCacheAction andThen googleAuthAction
}

object CommonActions {
  def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def noCache(result: Result): Result = result.withHeaders("Cache-Control" -> "no-cache, private", "Pragma" -> "no-cache")
}