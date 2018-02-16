package monitoring

import controllers.{Cached, NoCache}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.core.SourceMapper

import scala.concurrent._

class ErrorHandler(
  env: Environment,
  config: Configuration,
  sourceMapper: Option[SourceMapper],
  router: => Option[Router]
)(implicit executionContext: ExecutionContext) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    super.onClientError(request, statusCode, message).map(Cached(_))
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(NoCache(InternalServerError(views.html.error500(exception))))
  }
  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    logServerError(request, new PlayException("Bad request", "A bad request was received."))
    Future.successful(NoCache(BadRequest(views.html.error400(request, message))))
  }
}
