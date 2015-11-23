package services

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.soap.actions.subscribe.{CreditCardReferenceTransaction, Subscribe}
import com.gu.membership.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {
  case class CheckoutResult(salesforceMember: MemberId, userIdData: UserIdData, subscribeResult: SubscribeResult)
}

class CheckoutService(
       identityService: IdentityService,
       salesforceService: SalesforceService,
       paymentService: PaymentService,
       zuoraService: ZuoraService,
       exactTargetService: ExactTargetService
    ) extends LazyLogging {
  import CheckoutService.CheckoutResult

  def processSubscription(subscriptionData: SubscriptionData,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         ): Future[CheckoutResult] = {
    val personalData = subscriptionData.personalData

    def updateAuthenticatedUserDetails(): Unit =
      authenticatedUserOpt.foreach(identityService.updateUserDetails(personalData))

    def sendETDataExtensionRow(subscribeResult: SubscribeResult): Future[Unit] =
      exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData)

    val userOrElseRegisterGuest: Future[UserIdData] =
      authenticatedUserOpt.map(authenticatedUser => Future {
        RegisteredUser(authenticatedUser.user)
      }).getOrElse {
        logger.info(s"User does not have an Identity account. Creating a guest account")
        identityService.registerGuest(personalData)
      }

    def subscribeAction(payment: PaymentService#Payment): Future[Subscribe] =
      for {
        paymentMethod <- payment.makePaymentMethod
      } yield {
        Subscribe(
          account = payment.makeAccount,
          paymentMethodOpt = Some(paymentMethod),
          ratePlanId = subscriptionData.ratePlanId,
          firstName = personalData.firstName,
          lastName = personalData.lastName,
          address = personalData.address,
          paymentDelay = Some(zuoraService.paymentDelaysInDays),
          casIdOpt = None,
          ipAddressOpt = Some(requestData.ipAddress),
          featureIds = Nil
        )
      }

    for {
      userData <- userOrElseRegisterGuest
      memberId <- salesforceService.createOrUpdateUser(personalData, userData.id)
      payment = subscriptionData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          paymentService.makeDirectDebitPayment(paymentData, personalData, memberId)
        case paymentData@CreditCardData(_) =>
          paymentService.makeCreditCardPayment(paymentData, userData, memberId)
      }
      subscribe <- subscribeAction(payment)
      result <- zuoraService.subscribe(subscribe)
    } yield {
      updateAuthenticatedUserDetails()
      sendETDataExtensionRow(result)
      subscribe.paymentMethodOpt match {
        case Some(CreditCardReferenceTransaction(customer)) =>
          salesforceService.setCardInformation(userData.id, customer.id, customer.card.id)
      }
      CheckoutResult(memberId, userData, result)

    }
  }
}
