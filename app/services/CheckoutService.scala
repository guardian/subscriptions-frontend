package services

import com.gu.identity.play.IdMinimalUser
import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData
import play.api.mvc.Request

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckoutService(identityService: IdentityService) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData,
                          request: Request[_]): Future[Either[GuestUserNotCreated, IdMinimalUser]] =
    AuthenticationService.authenticatedUserFor(request).map(user => Future(Right(user)))
      .getOrElse {
        logger.info(s"User does not have an Identity account, creating a Guest one.")

        identityService.registerGuest(subscriptionData.personalData).map { guestFuture =>
          guestFuture.right.map(id => IdMinimalUser(id, None))
        }
      }
}

object CheckoutService extends CheckoutService(IdentityService)