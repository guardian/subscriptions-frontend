package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.MemberId
import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckoutService(identityService: IdentityService, salesforceService: SalesforceService) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData,
                          idUser: Option[IdMinimalUser]): Future[Option[MemberId]] =
    idUser.map(user => Future(Some(UserId(user.id))))
      .getOrElse {
        logger.info(s"User does not have an Identity account, creating a Guest one.")

        identityService.registerGuest(subscriptionData.personalData)
      }
      .flatMap(userId =>
        userId.map(salesforceService.createOrUpdateUser(subscriptionData.personalData, _))
          .getOrElse(Future(None)))
}

object CheckoutService extends CheckoutService(IdentityService, SalesforceService)