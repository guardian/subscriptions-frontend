package filters

import akka.stream.Materializer
import play.api.http.HeaderNames
import play.api.mvc._
import services.AsyncAuthenticationService
import utils.TestUsersService._

import scala.concurrent.{ExecutionContext, Future}

class AddGuIdentityHeaders(authenticationService: AsyncAuthenticationService)(implicit val mat: Materializer, val executionContext: ExecutionContext) extends Filter with HeaderNames {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    for {
      result <- nextFilter(requestHeader)
      userOpt <- authenticationService.tryAuthenticatedUserFor(requestHeader)
    } yield {
      (for {
        user <- userOpt
        cacheHeader <- result.header.headers.get(CACHE_CONTROL) if cacheHeader.contains("no-cache")
      } yield {
        result.withHeaders(
          "X-Gu-Identity-Id" -> user.id,
          "X-Gu-Membership-Test-User" -> SignedInUsername.passes(user).isDefined.toString
        )
      }).getOrElse(result)
    }

}
