package filters

import akka.stream.Materializer
import controllers.Cached.suitableForCaching
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class CheckCacheHeadersFilter (implicit val mat: Materializer, val executionContext: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      if (suitableForCaching(result)) {
        val hasCacheControl = result.header.headers.contains("Cache-Control")
        assert(hasCacheControl, s"Cache-Control not set. Ensure controller response has Cache-Control header set for ${requestHeader.path}. Throwing exception... ")
      }
      result
    }
  }
}
