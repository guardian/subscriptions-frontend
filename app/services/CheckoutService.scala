package services

import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Country
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.memsub.promo.PromotionApplicator._
import com.gu.memsub.promo._
import com.gu.memsub.services.PromoService
import com.gu.memsub.services.api.CatalogService
import com.gu.salesforce.ContactId
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService
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
import scalaz.{EitherT, Monad, NonEmptyList, \/}
import com.gu.memsub.promo.Promotion._

import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.std.option._

class CheckoutService(identityService: IdentityService,
                      salesforceService: SalesforceService,
                      paymentService: PaymentService,
                      catalogService: CatalogService,
                      zuoraService: ZuoraService,
                      exactTargetService: ExactTargetService,
                      zuoraProperties: ZuoraProperties,
                      promoService: PromoService,
                      promoPlans: DiscountRatePlanIds) extends LazyLogging {

  type NonFatalErrors = Seq[SubsError]
  type PostSubscribeResult = (Option[UserIdData], NonFatalErrors)
  type SubNel[A] = EitherT[Future, NonEmptyList[SubsError], A]
  type FatalErrors = NonEmptyList[SubsError]

  def processSubscription(subscriptionData: SubscribeRequest,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         )(implicit p: PromotionApplicator[NewUsers, Subscribe]): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] = {

    import subscriptionData.genericData._
    val plan = RatePlan(subscriptionData.productRatePlanId.get, None)
    def emailError: SubNel[Unit] = EitherT(Future.successful(\/.left(NonEmptyList(CheckoutIdentityFailure("Email in use")))))
    val idMinimalUser = authenticatedUserOpt.map(_.user)

    (for {
      userExists <- EitherT(IdentityService.doesUserExist(personalData.email).map(\/.right[FatalErrors, Boolean]))
      _ <- Monad[SubNel].whenM(userExists && authenticatedUserOpt.isEmpty)(emailError)
      memberId <- EitherT(createOrUpdateUserInSalesforce(personalData, idMinimalUser))
      purchaserIds = PurchaserIdentifiers(memberId, idMinimalUser)
      payment <- EitherT(createPaymentType(requestData, purchaserIds, subscriptionData))
      subscribe <- EitherT(createSubscribeRequest(personalData, requestData, plan, purchaserIds, payment))
      validPromotion = suppliedPromoCode.flatMap(promoService.validate[NewUsers](_, personalData.address.country.getOrElse(Country.UK), subscriptionData.productRatePlanId).toOption)
      withPromo = validPromotion.map(v => p.apply(v, subscribe, catalogService.digipackCatalog.unsafeFindPaid, promoPlans)).getOrElse(subscribe)
      result <- EitherT(createSubscription(withPromo, purchaserIds))
      out <- postSubscribeSteps(authenticatedUserOpt, memberId, result, subscriptionData, validPromotion)
    } yield CheckoutSuccess(memberId, out._1, result, validPromotion.map(_.code), out._2)).run
  }

  def postSubscribeSteps(user: Option[AuthenticatedIdUser],
                         contactId: ContactId,
                         result: SubscribeResult,
                         subscriptionData: SubscribeRequest,
                         promotion: Option[ValidPromotion[NewUsers]]): SubNel[PostSubscribeResult] = {

    val purchaserIds = PurchaserIdentifiers(contactId, user.map(_.user))
    val res = (
      storeIdentityDetails(subscriptionData.genericData.personalData, user, contactId).run |@|
      sendETDataExtensionRow(result, subscriptionData, gracePeriod(promotion), purchaserIds, promotion)
    ) { case(id, email) =>
      (id.toOption.map(_.userData), id.swap.toOption.toSeq.flatMap(_.list) ++ email.swap.toOption.toSeq.flatMap(_.list))
    }
    EitherT(res.map(\/.right[FatalErrors, PostSubscribeResult]))
  }

  private def gracePeriod(promo: Option[ValidPromotion[NewUsers]]) =
    promo.flatMap(_.promotion.asDiscount).fold(zuoraProperties.gracePeriodInDays)(_ => ZERO)

  private def gracePeriod(withPromo: Subscribe, subscribe: Subscribe) =
    if (withPromo.paymentDelay == subscribe.paymentDelay) zuoraProperties.gracePeriodInDays else ZERO

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
                                      subscriptionData: SubscribeRequest,
                                      gracePeriodInDays: Days,
                                      purchaserIds: PurchaserIdentifiers,
                                      validPromotion: Option[ValidPromotion[NewUsers]]): Future[NonEmptyList[SubsError] \/ Unit] =

    (for {
      a <- exactTargetService.sendETDataExtensionRow(subscribeResult, subscriptionData, gracePeriodInDays, validPromotion)
    } yield {
      \/.right(())
    }).recover {
      case e => \/.left(NonEmptyList(CheckoutExactTargetFailure(
        purchaserIds,
        s"ExactTarget failed to send welcome email to subscriber $purchaserIds")))
    }

  private def createOrUpdateUserInSalesforce(personalData: PersonalData, userData: Option[IdMinimalUser]): Future[NonEmptyList[SubsError] \/ ContactId] = {
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
      purchaserIds: PurchaserIdentifiers): Future[NonEmptyList[SubsError] \/ SubscribeResult] = {

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
      requestData: SubscriptionRequestData,
      purchaserIds: PurchaserIdentifiers,
      subscriptionData: SubscribeRequest): Future[NonEmptyList[SubsError] \/ PaymentService#Payment] = {

    try {
      val payment = subscriptionData.genericData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          paymentService.makeDirectDebitPayment(paymentData, subscriptionData.genericData.personalData, purchaserIds.memberId)
        case paymentData@CreditCardData(_) =>
          paymentService.makeCreditCardPayment(paymentData, subscriptionData.genericData.personalData, purchaserIds, subscriptionData.productData.fold(_.plan, _.plan))
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
