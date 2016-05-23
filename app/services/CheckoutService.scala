package services

import com.gu.config.DiscountRatePlanIds
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.memsub.services.PromoService
import com.gu.memsub.services.api.CatalogService
import com.gu.salesforce.ContactId
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.Lenses._
import com.gu.zuora.soap.models.Commands.{RatePlan, Subscribe}
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import model._
import model.error.CheckoutService._
import model.error.IdentityService._
import model.error.SubsError
import org.joda.time.Days
import org.joda.time.Days.ZERO
import touchpoint.ZuoraProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.{-\/, EitherT, NonEmptyList, \/, \/-}

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

    userBecomesSubscriber(authenticatedUserOpt, personalData, requestData, plan, subscriptionData)
  }

  private def gracePeriod(withPromo: Subscribe, subscribe: Subscribe) =
    if (withPromo.paymentDelay == subscribe.paymentDelay) zuoraProperties.gracePeriodInDays else ZERO

  private def userBecomesSubscriber(
       authenticatedUserOpt: Option[AuthenticatedIdUser],
       personalData: PersonalData,
       requestData: SubscriptionRequestData,
       plan: RatePlan,
       subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] = {

    val idMinimalUser = authenticatedUserOpt.map(_.user)

    (for {
      memberId <- EitherT(createOrUpdateUserInSalesforce(personalData, idMinimalUser, subscriptionData))
      purchaserIds = PurchaserIdentifiers(memberId, idMinimalUser)
      payment <- EitherT(createPaymentType(personalData, requestData, purchaserIds, subscriptionData))
      subscribe <- EitherT(createSubscribeRequest(personalData, requestData, plan, purchaserIds, payment))
      withPromo = promoService.applyPromotion(subscribe, subscriptionData.suppliedPromoCode, personalData.address.country)
      result <- EitherT(createSubscription(withPromo, purchaserIds, subscriptionData))
      identitySuccess <- storeIdentityDetails(personalData, authenticatedUserOpt, memberId)

      // exact target errors are non fatal so we can return what happened along with the checkout success
      result <- EitherT(sendETDataExtensionRow(result, subscriptionData, gracePeriod(subscribe, withPromo), purchaserIds).map( f =>
        \/.right(CheckoutSuccess(memberId, identitySuccess.userData, result, withPromo.promoCode, f.swap.toOption.map(_.head)))
      ))
    } yield result).run
  }

  private def storeIdentityDetails(
      personalData: PersonalData,
      authenticatedUserOpt: Option[AuthenticatedIdUser],
      memberId: ContactId): EitherT[Future, NonEmptyList[SubsError], IdentitySuccess] = {

    authenticatedUserOpt match {
      case Some(authenticatedIdUser) => {

        def addErrContext(errSeq: NonEmptyList[SubsError]): NonEmptyList[SubsError] =
          errSeq.<::(CheckoutIdentityFailure(
            s"Registered user ${authenticatedIdUser.user.id} could not become subscriber",
            Some(s"SF Account ID = ${memberId.salesforceAccountId}, SF Contact ID = ${memberId.salesforceContactId}")))

        EitherT(identityService.updateUserDetails(personalData)(authenticatedIdUser).map { _.leftMap(addErrContext(_)) })
      }

      case None => {
        logger.info(s"User does not have an Identity account. Creating a guest account")

        def addErrContext(errSeq: NonEmptyList[SubsError]): NonEmptyList[SubsError] =
          errSeq.<::(CheckoutIdentityFailure(
            s"Guest user (${personalData.toStringSanitized} could not become subscriber",
            Some(s"SF Account ID = ${memberId.salesforceAccountId}, SF Contact ID = ${memberId.salesforceContactId}")))

        for (identitySuccess <- EitherT(identityService.registerGuest(personalData).map { _.leftMap(addErrContext(_)) })) yield {
          val identityId = identitySuccess.userData.id.id
          val salesforceResult = salesforceService.repo.updateIdentityId(memberId, identityId)
          for (err <- EitherT(salesforceResult).swap) yield {
            // Swallow salesforce update errors, but log them
            logger.error(s"Error updating salesforce contact ${memberId.salesforceContactId} with identity id $identityId: ${err.getMessage()}")
          }

          identitySuccess
        }
      }
    }
  }

  private def sendETDataExtensionRow(
      subscribeResult: SubscribeResult,
      subscriptionData: SubscriptionData,
      gracePeriodInDays: Days,
      purchaserIds: PurchaserIdentifiers): Future[NonEmptyList[SubsError] \/ Unit] =

    (for {
      a <- exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData, gracePeriodInDays)
    } yield {
      \/.right(())
    }).recover {
      case e => \/.left(NonEmptyList(CheckoutExactTargetFailure(
        purchaserIds,
        s"ExactTarget failed to send welcome email to subscriber $purchaserIds")))
    }

  private def createOrUpdateUserInSalesforce(
      personalData: PersonalData,
      userData: Option[IdMinimalUser],
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ ContactId] = {

    (for {
      memberId <- salesforceService.createOrUpdateUser(personalData, userData)
    } yield {
      \/.right(memberId)
    }).recover {
      case e => \/.left(NonEmptyList(CheckoutSalesforceFailure(
        userData,
        s"${userData} could not subscribe during checkout because his details could not be updated in Salesforce")))
    }
  }

  private def createSubscribeRequest(
      personalData: PersonalData,
      requestData: SubscriptionRequestData,
      plan: RatePlan,
      purchaserIds: PurchaserIdentifiers,
      payment: PaymentService#Payment): Future[NonEmptyList[SubsError] \/ Subscribe] = {

    payment.makePaymentMethod.map { method =>
      \/.right(Subscribe(
        account = payment.makeAccount, Some(method),
        ratePlans = NonEmptyList(plan),
        name = personalData,
        address = personalData.address,
        paymentDelay = Some(zuoraProperties.paymentDelayInDays.plus(zuoraProperties.gracePeriodInDays)),
        ipAddress = requestData.ipAddress.map(_.getHostAddress)
      ))
    }.recover {
      case e: Stripe.Error => \/.left(NonEmptyList(CheckoutStripeError(
        purchaserIds,
        e,
        s"$purchaserIds could not subscribe during checkout due to Stripe API error")))

      case e => \/.left(NonEmptyList(CheckoutGenericFailure(
        purchaserIds,
        s"$purchaserIds could not subscribe during checkout")))
    }
  }

  private def createSubscription(
      subscribe: Subscribe,
      purchaserIds: PurchaserIdentifiers,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ SubscribeResult] = {

    zuoraService.createSubscription(subscribe).map(\/.right).recover {
      case e: PaymentGatewayError => \/.left(NonEmptyList(CheckoutZuoraPaymentGatewayError(
        purchaserIds,
        e,
        s"$purchaserIds could not subscribe during checkout due to Zuora Payment Gateway Error")))

      case e => \/.left(NonEmptyList(CheckoutGenericFailure(
        purchaserIds,
        s"$purchaserIds could not subscribe during checkout")))
    }
  }

  private def createPaymentType(
      personalData: PersonalData,
      requestData: SubscriptionRequestData,
      purchaserIds: PurchaserIdentifiers,
      subscriptionData: SubscriptionData): Future[NonEmptyList[SubsError] \/ PaymentService#Payment] = {

    try {
      val payment = subscriptionData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          paymentService.makeDirectDebitPayment(paymentData, personalData, purchaserIds.memberId)
        case paymentData@CreditCardData(_) =>
          val digipackPlan = catalogService.digipackCatalog.unsafeFind(subscriptionData.productRatePlanId)
          paymentService.makeCreditCardPayment(paymentData, personalData, purchaserIds, digipackPlan)
      }
      Future.successful(\/.right(payment))
    } catch {
      case e: Throwable => Future.successful(\/.left(NonEmptyList(CheckoutPaymentTypeFailure(
        purchaserIds,
        s"$purchaserIds could not subscribe during checkout because of a problem with selected payment type",
        None,
        Some(e.getMessage())))))
    }
  }
}
