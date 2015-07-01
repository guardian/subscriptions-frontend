package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.soap.Zuora.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Subscription(memberId: MemberId, zuoraSubscription: SubscribeResult)

class CheckoutService(identityService: IdentityService, salesforceService: SalesforceService) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData,
                          idUser: Option[IdMinimalUser]): Future[Subscription] = {
    def idUserF = idUser.map(user => Future(UserId(user.id)))
      .getOrElse {
      logger.info(s"User does not have an Identity account, creating a Guest one.")

      identityService.registerGuest(subscriptionData.personalData)
    }

    for {
      idUser <- idUserF
      memberId <- salesforceService.createOrUpdateUser(subscriptionData.personalData, idUser)
      subscription <- TouchpointBackend.Normal.zuoraService.createSubscription(memberId, subscriptionData)   //TODO when implementing test-users this requires updating to supply data to correct location
    } yield Subscription(memberId, subscription)

  }
}

object CheckoutService extends CheckoutService(IdentityService, SalesforceService)