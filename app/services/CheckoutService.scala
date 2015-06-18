package services

import com.gu.identity.play.IdMinimalUser
import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckoutService(identityService: IdentityService) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData,
                          idUser: Option[IdMinimalUser]): Future[Either[GuestUserNotCreated, IdMinimalUser]] =
    idUser.map(user => Future(Right(user)))
      .getOrElse {
        logger.info(s"User does not have an Identity account, creating a Guest one.")

        identityService.registerGuest(subscriptionData.personalData).map { guestFuture =>
          guestFuture.right.map(id => IdMinimalUser(id, None))
        }
      }
}

object CheckoutService extends CheckoutService(IdentityService)