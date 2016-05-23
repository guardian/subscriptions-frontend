package filters

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

object HandleXFrameOptionsOverrideHeader extends Filter {
  val HEADER_KEY = "X-Frame-Options-Override"

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.header.headers.get(HEADER_KEY).fold(result) { value =>
        result.withHeaders("X-Frame-Options" -> value)
      }
    }
  }
}
