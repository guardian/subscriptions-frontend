package actions

 import com.gu.googleauth
import com.typesafe.scalalogging.LazyLogging
import configuration.QA.passthroughCookie
import controllers.{Cached, NoCache}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import play.filters.csrf.{CSRFCheck, CSRFConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
trait CommonActions {

  val StoreAcquisitionDataAction = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(result => {
      request.getQueryString("acquisitionData").fold(result)(a => {
        val sessionWithAcquisitionData = request.session.data.toSeq ++ Seq("acquisitionData" -> a)
        result.withSession(sessionWithAcquisitionData: _*)
      })
    })
  }

  val NoCacheAction = StoreAcquisitionDataAction andThen resultModifier(noCache)

  val CachedAction = resultModifier(Cached(_))

  val CSRFCachedAsyncAction = (block: Request[_] => Future[Result]) => CSRFCheck(action = CachedAction.async(block), config = CSRFConfig.global.copy(checkContentType = (x: Option[String]) => true))

  def noCache(result: Result): Result = NoCache(result)

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }
}
