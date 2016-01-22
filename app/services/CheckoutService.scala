package services

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.memsub.services.api.CatalogService
import com.gu.salesforce.ContactId
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.typesafe.scalalogging.LazyLogging
import model._
import touchpoint.ZuoraProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {
  case class CheckoutResult(salesforceMember: ContactId, userIdData: UserIdData, subscribeResult: SubscribeResult)
}

class CheckoutService(identityService: IdentityService,
                      salesforceService: SalesforceService,
                      paymentService: PaymentService,
                      catalogService: CatalogService,
                      zuoraService: ZuoraService,
                      exactTargetService: ExactTargetService,
                      zuoraProperties: ZuoraProperties) extends LazyLogging {

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

    for {
      userData <- userOrElseRegisterGuest
      memberId <- salesforceService.createOrUpdateUser(personalData, userData.id)
      payment = subscriptionData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          paymentService.makeDirectDebitPayment(paymentData, personalData, memberId)
        case paymentData@CreditCardData(_) =>
          val plan = catalogService.digipackCatalog.unsafeFind(subscriptionData.productRatePlanId)
          paymentService.makeCreditCardPayment(paymentData, personalData, userData, memberId, plan)
      }
      method <- payment.makePaymentMethod
      result <- zuoraService.createSubscription(
        subscribeAccount = payment.makeAccount,
        paymentMethod = Some(method),
        productRatePlanId = subscriptionData.productRatePlanId,
        name = personalData,
        address = personalData.address,
        paymentDelay = Some(zuoraProperties.paymentDelayInDays),
        ipAddressOpt = requestData.ipAddress.map(_.getHostAddress))

    } yield {
      updateAuthenticatedUserDetails()
      sendETDataExtensionRow(result)
      CheckoutResult(memberId, userData, result)
    }
  }
}
