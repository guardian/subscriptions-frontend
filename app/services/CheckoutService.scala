package services

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.soap.models.Result.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import model.{PersonalData, SubscriptionData, SubscriptionRequestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {
  case class CheckoutResult(salesforceMember: MemberId, userIdData: UserIdData, zuoraResult: SubscribeResult)
}

class CheckoutService(
    identityService: IdentityService,
    salesforceService: SalesforceService,
    zuoraService: ZuoraService,
    exactTargetService: ExactTargetService
    ) extends LazyLogging {
  import CheckoutService.CheckoutResult

  def processSubscription(subscriptionData: SubscriptionData,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         ): Future[CheckoutResult] = {

    def updateAuthenticatedUserDetails(personalData: PersonalData): Unit = {
      for {
        authenticatedUser <- authenticatedUserOpt
      } yield {
        identityService.updateUserDetails(personalData, authenticatedUser)
      }
    }

    def sendETDataExtensionRow(subscribeResult: SubscribeResult): Future[Unit] =
      exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData, zuoraService)

    val userOrElseRegisterGuest: Future[UserIdData] =
      authenticatedUserOpt.map(authenticatedUser => Future {
        RegisteredUser(authenticatedUser.user)
      }).getOrElse {
        logger.info(s"User does not have an Identity account. Creating a guest account")
        identityService.registerGuest(subscriptionData.personalData)
      }

    for {
      userData <- userOrElseRegisterGuest
      memberId <- salesforceService.createOrUpdateUser(subscriptionData.personalData, userData.id)
      subscribeResult <- zuoraService.createSubscription(memberId, subscriptionData, requestData)
    } yield {
      updateAuthenticatedUserDetails(subscriptionData.personalData)
      sendETDataExtensionRow(subscribeResult)
      CheckoutResult(memberId, userData, subscribeResult)
    }
  }
}
