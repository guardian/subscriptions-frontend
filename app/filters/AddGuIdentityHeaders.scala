package filters

import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.AuthenticationService
import utils.TestUsers._

import scala.concurrent.Future

object AddGuIdentityHeaders extends Filter with HeaderNames {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
  } yield (for {
    cacheHeader <- result.header.headers.get(CACHE_CONTROL) if cacheHeader.contains("no-cache")
    user <- AuthenticationService.authenticatedUserFor(requestHeader)
  } yield result.withHeaders(
    "X-Gu-Identity-Id" -> user.id,
    "X-Gu-Membership-Test-User" -> SignedInUsername.passes(user).isDefined.toString
  )).getOrElse(result)

}
