package services

import com.gu.config.DiscountRatePlanIds
import com.gu.identity.play.AuthenticatedIdUser
import com.gu.memsub.services.PromoService
import com.gu.memsub.services.api.CatalogService
import com.gu.salesforce.ContactId
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.{Subscribe, RatePlan}
import com.gu.zuora.soap.models.Commands.Lenses._
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import model._
import model.error.CheckoutService._
import model.error.IdentityService._
import model.error.SubsError
import touchpoint.ZuoraProperties
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{NonEmptyList, EitherT, \/, -\/, \/-}
import scalaz.std.scalaFuture._

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
                         ): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] = {

    val personalData = subscriptionData.personalData
    val plan = RatePlan(subscriptionData.productRatePlanId.get, None)

    authenticatedUserOpt match {
      case Some(authenticatedIdUser) => registeredUserBecomesSubscriber(
        authenticatedUserOpt, personalData, RegisteredUser(authenticatedIdUser.user), requestData, plan, subscriptionData)

      case _ =>
        logger.info(s"User does not have an Identity account. Creating a guest account")

        def success(personalData: PersonalData, requestData: SubscriptionRequestData,
                    plan: RatePlan, subscriptionData: SubscriptionData)(identity: IdentitySuccess) =
          guestUserBecomesSubscriber(personalData, identity.userData, requestData, plan, subscriptionData)

        def failure(errSeq: NonEmptyList[SubsError]) = Future.successful(\/.left(errSeq.<::(CheckoutIdentityFailure(
          "User could not subscribe during checkout because Identity guest account could not be created",
          subscriptionData.toString,
          None))))

        identityService.registerGuest(personalData).flatMap{_.fold(
          failure, success(personalData, requestData, plan, subscriptionData))}
    }
  }

  private def registeredUserBecomesSubscriber(
       authenticatedUserOpt: Option[AuthenticatedIdUser],
       personalData: PersonalData,
       userData: UserIdData,
       requestData: SubscriptionRequestData,
       plan: RatePlan,
       subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] =

    (for {
      memberId <- EitherT(createOrUpdateUser(personalData, userData, subscriptionData))
      subscribe <- EitherT(createSubscribeRequest(personalData, requestData, plan, userData, memberId, subscriptionData))
      result <- EitherT(createSubscription(subscribe, userData, subscriptionData))
      _ <- EitherT(updateAuthenticatedUserDetails(authenticatedUserOpt, personalData, subscriptionData))
      _ <- EitherT(sendETDataExtensionRow(result, subscriptionData))
    } yield {
      CheckoutSuccess(memberId, userData, result, subscribe.promoCode)
    }).run

  private def guestUserBecomesSubscriber(
      personalData: PersonalData,
      userData: UserIdData,
      requestData: SubscriptionRequestData,
      plan: RatePlan,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] =

    (for {
      memberId <- EitherT(createOrUpdateUser(personalData, userData, subscriptionData))
      subscribe <- EitherT(createSubscribeRequest(personalData, requestData, plan, userData, memberId, subscriptionData))
      result <- EitherT(createSubscription(subscribe, userData, subscriptionData))
      _ <- EitherT(sendETDataExtensionRow(result, subscriptionData))
    } yield {
      CheckoutSuccess(memberId, userData, result, subscribe.promoCode)
    }).run

  private def updateAuthenticatedUserDetails(
      authenticatedUserOpt: Option[AuthenticatedIdUser],
      personalData: PersonalData,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ IdentitySuccess] =

    identityService.updateUserDetails(personalData)(authenticatedUserOpt.get).map {
      case \/-(IdentitySuccess(user)) => \/.right(IdentitySuccess(user))

      case -\/(errSeq) =>
        \/.left(errSeq.<::(CheckoutIdentityFailure(
          "Registered user could not subscribe during checkout because Identity details could not be updated",
          subscriptionData.toString,
          None)))
    }

  private def sendETDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ Unit] =

    (for {
      a <- exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData)
    } yield {
      \/.right(())
    }).recover {
      case e => \/.left(NonEmptyList(CheckoutExactTargetFailure(
        s"User could not subscribe during checkout because ExactTarget failed to send the subscription email",
        subscriptionData.toString,
        None)))
    }

  private def createOrUpdateUser(
      personalData: PersonalData,
      userData: UserIdData,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ ContactId] = {

    (for {
      memberId <- salesforceService.createOrUpdateUser(personalData, userData.id)
    } yield {
      \/.right(memberId)
    }).recover {
      case e => \/.left(NonEmptyList(CheckoutSalesforceFailure(
        userData.id.id,
        s"User ${userData.id.id} could not subscribe during checkout because his details could not be updated in Salesforce",
        subscriptionData.toString,
        None)))
    }
  }

  private def createSubscribeRequest(
     personalData: PersonalData, requestData: SubscriptionRequestData,
     plan: RatePlan,
     userData: UserIdData,
     memberId: ContactId,
     subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ Subscribe] = {

    val payment = subscriptionData.paymentData match {
      case paymentData@DirectDebitData(_, _, _) =>
        paymentService.makeDirectDebitPayment(paymentData, personalData, memberId)
      case paymentData@CreditCardData(_) =>
        val plan = catalogService.digipackCatalog.unsafeFind(subscriptionData.productRatePlanId)
        paymentService.makeCreditCardPayment(paymentData, personalData, userData, memberId, plan)
    }

    (for {
      method <- payment.makePaymentMethod
      subscribe = promoService.applyPromotion(
        Subscribe(
          account = payment.makeAccount,
          paymentMethod = Some(method),
          ratePlans = NonEmptyList(plan),
          name = personalData,
          address = personalData.address,
          paymentDelay = Some(zuoraProperties.paymentDelayInDays),
          ipAddress = requestData.ipAddress.map(_.getHostAddress)
        ),
        subscriptionData.suppliedPromoCode,
        Some(personalData.country)
      )
    } yield {
      \/.right(subscribe)
    }).recover {
      case e: Stripe.Error => \/.left(NonEmptyList(CheckoutStripeError(
        userData.id.id,
        e,
        s"User ${userData.id.id} could not subscribe during checkout due to Stripe API error",
        subscriptionData.toString,
        None)))

      case e => \/.left(NonEmptyList(CheckoutGenericFailure(
        userData.id.id,
        s"User ${userData.id.id} could not subscribe during checkout",
        subscriptionData.toString,
        None)))
    }
  }

  private def createSubscription(
      subscribe: Subscribe,
      userData: UserIdData,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ SubscribeResult] = {

    zuoraService.createSubscription(subscribe).map(\/.right).recover {
      case e: PaymentGatewayError => \/.left(NonEmptyList(CheckoutZuoraPaymentGatewayError(
        userData.id.id,
        e,
        s"User ${userData.id.id} could not subscribe during checkout due to Zuora Payment Gateway Error",
        subscriptionData.toString,
        None)))

      case e => \/.left(NonEmptyList(CheckoutGenericFailure(
        userData.id.id,
        s"User ${userData.id.id} could not subscribe during checkout",
        subscriptionData.toString,
        None)))
    }
  }
}
