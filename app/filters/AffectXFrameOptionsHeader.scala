package filters

import configuration.Config
import controllers.Cached._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.Future

object AffectXFrameOptionsHeader extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      if (requestHeader.path.startsWith("/q/")) {
        result.withHeaders("X-Frame-Options" -> s"ALLOW ${Config.previewXFrameOptionsOverride}")
      } else {
        result
      }
    }
  }
}
