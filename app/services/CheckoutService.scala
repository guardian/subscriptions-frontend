package services

import com.gu.config.DiscountRatePlanIds
import com.gu.identity.play.AuthenticatedIdUser
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.services.PromoService
import com.gu.memsub.services.api.CatalogService
import com.gu.salesforce.ContactId
import com.gu.stripe.Stripe
import com.gu.subscriptions.Discounter
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.{Subscribe, RatePlan}
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import model._
import services.CheckoutService._
import services.IdentityService.{IdentityFailure, IdentitySuccess, IdentityResult}
import touchpoint.ZuoraProperties
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {
  sealed trait CheckoutResult

  case class CheckoutSuccess(
      salesforceMember: ContactId,
      userIdData: UserIdData,
      subscribeResult: SubscribeResult,
      validPromoCode: Option[PromoCode]) extends CheckoutResult

  case class CheckoutIdentityFailure(
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutGenericFailure(
      userId: String,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutStripeError(
      userId: String,
      paymentError: Throwable,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutZuoraPaymentGatewayError(
      userId: String,
      paymentError: PaymentGatewayError,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class ProcessSubscriptionIn(subscriptionData: SubscriptionData,
                                   authenticatedUserOpt: Option[AuthenticatedIdUser],
                                   requestData: SubscriptionRequestData)
}

class CheckoutService(identityService: IdentityService,
                      salesforceService: SalesforceService,
                      paymentService: PaymentService,
                      catalogService: CatalogService,
                      zuoraService: ZuoraService,
                      exactTargetService: ExactTargetService,
                      zuoraProperties: ZuoraProperties,
                      promoService: PromoService,
                      promoPlans: DiscountRatePlanIds) extends LazyLogging {

  def processSubscription(subscriptionData: SubscriptionData,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         ): Future[Either[Seq[SubsError], CheckoutSuccess]] = {

    val personalData = subscriptionData.personalData

    val plan = RatePlan(subscriptionData.productRatePlanId.get, None)
    val discounter = new Discounter(promoPlans, promoService, catalogService.digipackCatalog)

    val validPromoCode = for {
      code <- subscriptionData.suppliedPromoCode
      promotion <- promoService.findPromotion(code)
      if promotion.validateFor(subscriptionData.productRatePlanId, personalData.address.country).isRight
    } yield code

    def updateAuthenticatedUserDetails(): Unit =
      authenticatedUserOpt.foreach(identityService.updateUserDetails(personalData))

    def sendETDataExtensionRow(subscribeResult: SubscribeResult): Future[Unit] =
      exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData)

    def userBecomesSubscriber(userData: UserIdData): Future[Either[Seq[SubsError], CheckoutSuccess]] = {
        (for {
          memberId <- salesforceService.createOrUpdateUser(personalData, userData.id)
          payment = subscriptionData.paymentData match {
            case paymentData@DirectDebitData(_, _, _) =>
              paymentService.makeDirectDebitPayment(paymentData, personalData, memberId)
            case paymentData@CreditCardData(_) =>
              val plan = catalogService.digipackCatalog.unsafeFind(subscriptionData.productRatePlanId)
              paymentService.makeCreditCardPayment(paymentData, personalData, userData, memberId, plan)
          }
          method <- payment.makePaymentMethod
          result <- zuoraService.createSubscription(Subscribe(
            account = payment.makeAccount,
            paymentMethod = Some(method),
            ratePlans = discounter.applyPromoCode(plan, validPromoCode),
            name = personalData,
            address = personalData.address,
            promoCode = validPromoCode,
            paymentDelay = Some(zuoraProperties.paymentDelayInDays),
            ipAddress = requestData.ipAddress.map(_.getHostAddress)))

        } yield {
          updateAuthenticatedUserDetails()
          sendETDataExtensionRow(result)
          Right(CheckoutSuccess(memberId, userData, result, validPromoCode))
        }).recover {

          case e: Stripe.Error => Left(Seq(CheckoutStripeError(
            userData.id.id,
            e,
            s"User ${userData.id.id} could not subscribe during checkout due to Stripe API error",
            ProcessSubscriptionIn(subscriptionData, authenticatedUserOpt, requestData).toString,
            None)))

          case e: PaymentGatewayError => Left(Seq(CheckoutZuoraPaymentGatewayError(
            userData.id.id,
            e,
            s"User ${userData.id.id} could not subscribe during checkout due to Zuora Payment Gateway Error",
            ProcessSubscriptionIn(subscriptionData, authenticatedUserOpt, requestData).toString,
            None)))

          case e => Left(Seq(CheckoutGenericFailure(
            userData.id.id,
            s"User ${userData.id.id} could not subscribe during checkout",
            ProcessSubscriptionIn(subscriptionData, authenticatedUserOpt, requestData).toString,
            None)))
        }
    }

    authenticatedUserOpt match {
      case Some(authenticatedIdUser) =>
        userBecomesSubscriber(RegisteredUser(authenticatedIdUser.user))

      case _ =>
        logger.info(s"User does not have an Identity account. Creating a guest account")
        identityService.registerGuest(personalData).flatMap {
            case Right(IdentitySuccess(guestUser)) => userBecomesSubscriber(guestUser)

            case Left(errSeq) =>
              Future.successful(

                Left(errSeq.+:(CheckoutIdentityFailure(
                  "User could not subscribe during checkout becuase Identity guest account could not be created",
                  ProcessSubscriptionIn(subscriptionData, authenticatedUserOpt, requestData).toString,
                  None))))
        }
    }
  }
}
