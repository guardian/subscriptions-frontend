package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.soap.Zuora.SubscribeResult
import TouchpointBackend.{Normal => touchpointBackend}
import play.api.Logger
import touchpointBackend.ratePlan.{ratePlanId => ratePlan}
import com.typesafe.scalalogging.LazyLogging
import model.{PersonalData, SubscriptionData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {
  case class CheckoutResult(salesforceMember: MemberId, userIdData: UserIdData, zuoraResult: SubscribeResult)
}

class CheckoutService(identityService: IdentityService, salesforceService: SalesforceService, zuoraService: ZuoraService) extends LazyLogging {
  import CheckoutService.CheckoutResult

  def processSubscription(subscriptionData: SubscriptionData,
                          idUserOpt: Option[IdMinimalUser],
                          authCookieOpt: Option[AuthCookie]): Future[CheckoutResult] = {

    def updateAuthenticatedUserDetails(personalData: PersonalData): Unit = {
      for {
        user <- idUserOpt
        authCookie <- authCookieOpt
      } yield {
        identityService.updateUserDetails(personalData, UserId(user.id), authCookie)
      }
    }

    val userOrElseRegisterGuest: Future[UserIdData] =
      idUserOpt.map(user => Future {
        RegisteredUser(user)
      }).getOrElse {
        logger.info(s"User does not have an Identity account. Creating a guest account")
        identityService.registerGuest(subscriptionData.personalData)
      }

    userOrElseRegisterGuest.onComplete(t => Logger.info("userOrElseRegisterGuest result "+t))


    //TODO when implementing test-users this requires updating to supply data to correct location
    for {
      userData <- userOrElseRegisterGuest
      memberId <- {
        val mi = salesforceService.createOrUpdateUser(subscriptionData.personalData, userData.id)
        mi.onComplete(t => Logger.info("salesforce result "+t))
        mi
      }
      subscribeResult <- {
        val sr = zuoraService.createSubscription(memberId, subscriptionData)
        sr.onComplete(t => Logger.info("zuora result " + t))
        sr
      }
    } yield {
      Logger.info(s"Identity user : ${userData.id}")
      Logger.info(s"Salesforce user: ${memberId.salesforceContactId}")
      Logger.info(s"Zuora: ${subscribeResult.id}")
      updateAuthenticatedUserDetails(subscriptionData.personalData)
      CheckoutResult(memberId, userData, subscribeResult)
    }
  }
}
