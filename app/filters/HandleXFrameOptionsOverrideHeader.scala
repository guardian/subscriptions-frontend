package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
object HandleXFrameOptionsOverrideHeader {
  val HEADER_KEY = "X-Frame-Options-Override"
}
class HandleXFrameOptionsOverrideHeader (implicit val mat: Materializer, executionContext: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.header.headers.get(HandleXFrameOptionsOverrideHeader.HEADER_KEY).fold(result) { value =>
        result.withHeaders("X-Frame-Options" -> value)
      }
    }
  }
}
