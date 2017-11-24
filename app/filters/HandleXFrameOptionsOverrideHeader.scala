package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
object HandleXFrameOptionsOverrideHeader {
  val HEADER_KEY = "X-Frame-Options-Override"
}
class HandleXFrameOptionsOverrideHeader @Inject()(implicit  val mat: Materializer) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.header.headers.get(HandleXFrameOptionsOverrideHeader.HEADER_KEY).fold(result) { value =>
        result.withHeaders("X-Frame-Options" -> value)
      }
    }
  }
}
