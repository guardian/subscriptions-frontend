package filters

import configuration.Config
import controllers.Cached._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.mvc._

import scala.concurrent.Future

object HandleXFrameOptionsOverrideHeader extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.header.headers.get("X-Frame-Options-Override").fold(result) { value =>
        result.withHeaders("X-Frame-Options" -> value)
      }
    }
  }
}
