package services

import com.gu.identity.play.{IdMinimalUser, IdUser}

import scala.concurrent.Future

import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData
import play.api.mvc.Request
import scala.concurrent.ExecutionContext.Implicits.global

class CheckoutService(identityService: IdentityService) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData, request: Request[_]): Future[Either[GuestUserNotCreated, IdMinimalUser]] =  {

    val lookupUser = AuthenticationService.authenticatedUserFor(request)

    if(lookupUser.isDefined) Future(Right(lookupUser.get))
    else {
      logger.info(s"User does not have an Identity account, creating a Guest one.")

      identityService.registerGuest(subscriptionData.personalData).map { guestFuture =>
        guestFuture.right.map(id => IdMinimalUser(id, None))
      }
    }
  }
}

object CheckoutService extends CheckoutService(IdentityService)