package monitoring

import javax.inject._

import controllers.{Cached, NoCache}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                               ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    super.onClientError(request, statusCode, message).map(Cached(_))
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(NoCache(InternalServerError(views.html.error500(exception))))
}
