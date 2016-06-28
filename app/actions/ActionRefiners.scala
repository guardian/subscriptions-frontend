package actions
import com.gu.memsub.{Subscription, Digipack}
import play.api.mvc.{ActionFilter, Result, Request}
import scala.concurrent.ExecutionContext.Implicits.global
import services.AuthenticationService._
import services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie
import scalaz.std.scalaFuture._
import scala.concurrent.Future
import scalaz.OptionT


object ActionRefiners {

  def noSubscriptionAction(onSubscription: Subscription => Result): ActionFilter[Request]  = new ActionFilter[Request] {
    override def filter[A](request: Request[A]): Future[Option[Result]] = {
      authenticatedUserFor(request).fold[Future[Option[Result]]] {Future.successful(None)} { user =>
        val tp = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)(request).backend
        (for {
          sf <- OptionT(tp.salesforceService.repo.get(user.id))
          sub <- OptionT(tp.subscriptionService.get(sf))
        } yield onSubscription(sub)).run
      }
    }
  }
}
