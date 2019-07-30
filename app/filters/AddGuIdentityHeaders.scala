package filters

import akka.stream.Materializer
import play.api.http.HeaderNames
import play.api.mvc._
import services.AuthenticationService
import utils.TestUsers._

import scala.concurrent.{ExecutionContext, Future}

class AddGuIdentityHeaders(authenticationService: AuthenticationService) (implicit val mat: Materializer, val executionContext: ExecutionContext) extends Filter with HeaderNames {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
  } yield (for {
    cacheHeader <- result.header.headers.get(CACHE_CONTROL) if cacheHeader.contains("no-cache")
    user <- authenticationService.authenticatedUserFor(requestHeader)
  } yield result.withHeaders(
    "X-Gu-Identity-Id" -> user.user.id,
    "X-Gu-Membership-Test-User" -> SignedInUsername.passes(user.user).isDefined.toString
  )).getOrElse(result)

}
