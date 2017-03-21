package services

import com.github.nscala_time.time.OrderingImplicits.LocalDateOrdering
import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Currency.GBP
import com.gu.i18n.{Country, CountryGroup}
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{Renewal, ValidPromotion, _}
import com.gu.memsub.services.{GetSalesforceContactForSub, PromoService, PaymentService => CommonPaymentService}
import com.gu.memsub.subsv2.SubscriptionPlan.WeeklyPlan
import com.gu.memsub.subsv2.{Catalog, ReaderType, Subscription}
import com.gu.memsub.{Address, Product}
import com.gu.salesforce.{Contact, ContactId}
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.{Account, PaymentMethod, RatePlan, Subscribe, _}
import com.gu.zuora.soap.models.Queries
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors._
import logging.ContextLogging
import model.BillingPeriodOps._
import model.SubscriptionOps._
import model.error.CheckoutService._
import model.error.IdentityService._
import model.error.SubsError
import model.{Renewal, _}
import org.joda.time.LocalDate.now
import org.joda.time.{DateTimeConstants, Days, LocalDate}
import touchpoint.ZuoraProperties
import views.support.Pricing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.{EitherT, Monad, NonEmptyList, \/}

object CheckoutService {
  def fulfilmentDelay(in: Either[PaperData, DigipackData])(implicit now: LocalDate): Days = in.fold(
    p => Days.daysBetween(now, p.startDate), d => Days.ZERO
  )
  def paymentDelay(in: Either[PaperData, DigipackData], zuora: ZuoraProperties)(implicit now: LocalDate): Days = in.fold(
    p => Days.daysBetween(now, p.startDate), d => zuora.gracePeriodInDays.plus(zuora.defaultDigitalPackFreeTrialPeriod)
  )
  def nextFriday(d: LocalDate):LocalDate = d match {
    case weekday if d.getDayOfWeek < DateTimeConstants.SATURDAY => d.withDayOfWeek(DateTimeConstants.FRIDAY)
    case weekend => d.plusWeeks(1).withDayOfWeek(DateTimeConstants.FRIDAY)
  }

  def determineFirstAvailableWeeklyDate(now: LocalDate): LocalDate = {
    val initialFulfilment = new LocalDate("2017-03-31")
    val nextAvailableDate = nextFriday(now plusWeeks 1)
    if(nextAvailableDate isAfter initialFulfilment) nextAvailableDate else initialFulfilment
  }

}

class CheckoutService(identityService: IdentityService,
                      salesforceService: SalesforceService,
                      paymentService: PaymentService,
                      catalog: Catalog,
                      zuoraService: ZuoraService,
                      exactTargetService: ExactTargetService,
                      zuoraProperties: ZuoraProperties,
                      promoService: PromoService,
                      promoPlans: DiscountRatePlanIds,
                      commonPaymentService: CommonPaymentService) extends ContextLogging {

  type NonFatalErrors = Seq[SubsError]
  type PostSubscribeResult = (Option[UserIdData], NonFatalErrors)
  type SubNel[A] = EitherT[Future, NonEmptyList[SubsError], A]
  type FatalErrors = NonEmptyList[SubsError]

  private def isGuardianWeekly(paperData: PaperData): Boolean = paperData.plan.product match {
    case _:Product.Weekly => true
    case _ => false
  }

  private val allSixWeekAssociations = List(catalog.weekly.zoneA.associations, catalog.weekly.zoneC.associations).flatten

  // This method is not genericised because the '6' is not stored in the association.
  def processSixWeekIntroductoryPeriod(daysUntilFirstIssue: Days, originalCommand: Subscribe): Subscribe = {
    val maybeSixWeekAssociation = allSixWeekAssociations.find(_._1.id.get == originalCommand.ratePlans.head.productRatePlanId)
    val updatedCommand = maybeSixWeekAssociation.map { case (sixWeekPlan, recurringPlan) =>
      val sixWeeksPlanStartDate = originalCommand.contractEffective.plusDays(daysUntilFirstIssue.getDays)
      val replacementPlans = NonEmptyList(
        RatePlan(
          sixWeekPlan.id.get,
          Some(ChargeOverride(sixWeekPlan.charges.chargeId.get, triggerDate = Some(sixWeeksPlanStartDate)))
        ),
        RatePlan(recurringPlan.id.get, None)
      )
      val updatedContractAcceptance = sixWeeksPlanStartDate.plusWeeks(6)
      originalCommand.copy(ratePlans = replacementPlans, contractAcceptance = updatedContractAcceptance)
    }
    updatedCommand.getOrElse(originalCommand)
  }

  def processSubscription(subscriptionData: SubscribeRequest,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         )(implicit p: PromotionApplicator[NewUsers, Subscribe]): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] = {

    import subscriptionData.genericData._
    val plan = RatePlan(subscriptionData.productRatePlanId.get, None)
    def emailError: SubNel[Unit] = EitherT(Future.successful(\/.left(NonEmptyList(CheckoutIdentityFailure("Email in use")))))
    val idMinimalUser = authenticatedUserOpt.map(_.user)
    val soldToContact = subscriptionData.productData.left.toOption.filter(isGuardianWeekly).map(_.deliveryAddress)

    implicit val today = now()

    (for {
      userExists <- EitherT(IdentityService.doesUserExist(personalData.email).map(\/.right[FatalErrors, Boolean]))
      _ <- Monad[SubNel].whenM(userExists && authenticatedUserOpt.isEmpty)(emailError)
      memberId <- EitherT(createOrUpdateUserInSalesforce(subscriptionData, idMinimalUser))
      purchaserIds = PurchaserIdentifiers(memberId, idMinimalUser)
      payment <- EitherT(createPaymentType(purchaserIds, subscriptionData))
      fulfilmentDelay = CheckoutService.fulfilmentDelay(subscriptionData.productData)
      paymentDelay = CheckoutService.paymentDelay(subscriptionData.productData, zuoraProperties)
      paymentMethod <- EitherT(attachPaymentMethodToStripeCustomer(payment, purchaserIds))
      initialCommand = createSubscribeRequest(personalData, soldToContact, requestData, plan, purchaserIds, paymentMethod, payment.makeAccount, Some(fulfilmentDelay), Some(paymentDelay))
      subscribe = processSixWeekIntroductoryPeriod(fulfilmentDelay, initialCommand)
      validPromotion = promoCode.flatMap(promoService.validate[NewUsers](_, personalData.address.country.getOrElse(Country.UK), subscriptionData.productRatePlanId).toOption)
      withPromo = validPromotion.map(v => p.apply(v, prpId => catalog.paid.find(_.id == prpId).map(_.charges.billingPeriod).get, promoPlans)(subscribe)).getOrElse(subscribe)
      result <- EitherT(createSubscription(withPromo, purchaserIds))
      out <- postSubscribeSteps(authenticatedUserOpt, memberId, result, subscriptionData, validPromotion)
    } yield CheckoutSuccess(memberId, out._1, result, validPromotion, out._2)).run
  }

  def postSubscribeSteps(user: Option[AuthenticatedIdUser],
                         contactId: ContactId,
                         result: SubscribeResult,
                         subscriptionData: SubscribeRequest,
                         promotion: Option[ValidPromotion[NewUsers]]): SubNel[PostSubscribeResult] = {

    val purchaserIds = PurchaserIdentifiers(contactId, user.map(_.user))
    val res = (
      storeIdentityDetails(subscriptionData, user, contactId).run |@|
      sendETDataExtensionRow(result, subscriptionData, gracePeriod(promotion), purchaserIds, promotion)
    ) { case(id, email) =>
      (id.toOption.map(_.userData), id.swap.toOption.toSeq.flatMap(_.list) ++ email.swap.toOption.toSeq.flatMap(_.list))
    }
    EitherT(res.map(\/.right[FatalErrors, PostSubscribeResult]))
  }

  private def gracePeriod(promo: Option[ValidPromotion[NewUsers]]) =
    promo.flatMap(_.promotion.asDiscount).fold(zuoraProperties.gracePeriodInDays)(_ => Days.ZERO)

  private def storeIdentityDetails(
      subscribeRequest: SubscribeRequest,
      authenticatedUserOpt: Option[AuthenticatedIdUser],
      memberId: ContactId): EitherT[Future, NonEmptyList[SubsError], IdentitySuccess] = {

    val personalData = subscribeRequest.genericData.personalData
    val deliveryAddress = subscribeRequest.productData.left.toOption.map(_.deliveryAddress)


    def addErrContext(context: String)(errSeq: NonEmptyList[SubsError]): NonEmptyList[SubsError] =
      errSeq.<::(CheckoutIdentityFailure(
        s"$context user ${authenticatedUserOpt.map(_.user.id)} could not become subscriber",
        Some(s"SF Account ID = ${memberId.salesforceAccountId}, SF Contact ID = ${memberId.salesforceContactId}")
      ))

    authenticatedUserOpt match {
      case Some(authenticatedIdUser) =>
        EitherT(identityService.updateUserDetails(personalData, deliveryAddress)(authenticatedIdUser)).leftMap(addErrContext("Authenticated"))
      case None =>
        logger.info(s"User does not have an Identity account. Creating a guest account")
        EitherT(identityService.registerGuest(personalData, deliveryAddress)).leftMap(addErrContext("Guest")).map { succ =>
          EitherT(salesforceService.repo.updateIdentityId(memberId, succ.userData.id.id)).swap.map(err =>
            logger.error(s"Error updating salesforce contact ${memberId.salesforceContactId} with identity id ${succ.userData.id.id}: ${err.getMessage}")
          )
          succ
        }
    }
  }

  private def sendETDataExtensionRow(
    subscribeResult: SubscribeResult,
    subscriptionData: SubscribeRequest,
    gracePeriodInDays: Days,
    purchaserIds: PurchaserIdentifiers,
    validPromotion: Option[ValidPromotion[NewUsers]]
  ): Future[NonEmptyList[SubsError] \/ Unit] =

    (for {
      a <- exactTargetService.enqueueETWelcomeEmail(subscribeResult, subscriptionData, gracePeriodInDays, validPromotion, purchaserIds)
    } yield {
      \/.right(())
    }).recover {
      case e: Throwable => \/.left(NonEmptyList(CheckoutExactTargetFailure(
        purchaserIds,
        s"ExactTarget failed to send welcome email to subscriber $purchaserIds: ${e.getMessage}")))
    }

  private def createOrUpdateUserInSalesforce(subscribeRequest: SubscribeRequest, userData: Option[IdMinimalUser]): Future[NonEmptyList[SubsError] \/ ContactId] = {
    (for {
      memberId <- salesforceService.createOrUpdateUser(
        subscribeRequest.genericData.personalData,
        subscribeRequest.productData.left.toOption,
        userData
      )
    } yield {
      \/.right(memberId)
    }).recover {
      case e => {
        val message = s"${userData} could not subscribe during checkout because his details could not be updated in Salesforce"
        logger.error(s"$message: ${e.toString}")
        \/.left(NonEmptyList(CheckoutSalesforceFailure(
          userData,
          message)))
      }
    }
  }

  private def attachPaymentMethodToStripeCustomer(payment: PaymentService#Payment, purchaserIds: PurchaserIdentifiers): Future[NonEmptyList[SubsError] \/ PaymentMethod] =
    payment.makePaymentMethod.map(\/.right).recover {
        case e: Stripe.Error =>
          \/.left(NonEmptyList(CheckoutStripeError(
            purchaserIds,
            e,
            s"$purchaserIds could not subscribe because payment method could not be attached to Stripe customer",
            errorResponse = Some(e.getMessage))))

        case e => \/.left(NonEmptyList(CheckoutGenericFailure(
          purchaserIds,
          s"$purchaserIds could not subscribe during checkout")))
      }

  private def createSubscribeRequest(
      personalData: PersonalData,
      soldToContact: Option[Address],
      requestData: SubscriptionRequestData,
      plan: RatePlan,
      purchaserIds: PurchaserIdentifiers,
      paymentMethod: PaymentMethod,
      acc: Account,
      fufilmentDelay: Option[Days],
      paymentDelay: Option[Days]): Subscribe = {

    val acquisitionDate = now
    val fulfilmentDate = fufilmentDelay.map(delay => now.plusDays(delay.getDays)).getOrElse(acquisitionDate)
    val firstPaymentDate = paymentDelay.map(delay => now.plusDays(delay.getDays)).getOrElse(fulfilmentDate)

    Subscribe(
      account = acc,
      paymentMethod = Some(paymentMethod),
      ratePlans = NonEmptyList(plan),
      name = personalData,
      address = personalData.address,
      email = personalData.email,
      soldToContact = soldToContact.map(address => SoldToContact(
        name = personalData,        // TODO once we have gifting change this to the Giftee's name
        address = address,
        email = personalData.email  // TODO once we have gifting change this to the Giftee's email address
      )),
      contractEffective = acquisitionDate,
      contractAcceptance = firstPaymentDate,
      supplierCode = requestData.supplierCode,
      ipCountry = requestData.ipCountry
    )
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

  private def createPaymentType( purchaserIds: PurchaserIdentifiers, subscriptionData: SubscribeRequest): Future[NonEmptyList[SubsError] \/ PaymentService#Payment] = {

    try {
      val payment = subscriptionData.genericData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          val personalData = subscriptionData.genericData.personalData
          require(personalData.address.country.contains(Country.UK), "Direct Debit payment only works in the UK right now")
          paymentService.makeDirectDebitPayment(paymentData, personalData.first, personalData.last, purchaserIds.memberId)
        case paymentData@CreditCardData(_) =>
          val plan = subscriptionData.productData.fold(_.plan, _.plan)
          val desiredCurrency = subscriptionData.genericData.currency
          val currency = if (plan.charges.price.currencies.contains(desiredCurrency)) desiredCurrency else GBP
          paymentService.makeCreditCardPayment(paymentData, currency, purchaserIds)
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

  def renewSubscription(subscription: Subscription[WeeklyPlanOneOff], renewal: model.Renewal)
    (implicit p: PromotionApplicator[com.gu.memsub.promo.Renewal, Renew]) = {

    implicit val context = subscription

    def getPayment(contact: Contact, billto: Queries.Contact): PaymentService#Payment = {
      val idMinimalUser = IdMinimalUser(contact.identityId, None)
      val pid = PurchaserIdentifiers(contact, Some(idMinimalUser))
      renewal.paymentData match {
        case cd: CreditCardData => paymentService.makeCreditCardPayment(cd, subscription.currency, pid)
        case dd: DirectDebitData => paymentService.makeDirectDebitPayment(dd, billto.firstName, billto.lastName, contact)
      }
    }

    val currentVersionExpired = subscription.termEndDate.isBefore(now).withContextLogging("currentVersionExpired")

    // For a renewal, all dates should be identical. If the sub has expired, this date should be fast-forwarded to the next available paper date
    val newTermStartDate = {
      if (currentVersionExpired) CheckoutService.determineFirstAvailableWeeklyDate(now) else subscription.termEndDate
    }.withContextLogging("startDateForRenewal")

    def constructRenewCommand(maybeValidPromo: Option[ValidPromotion[com.gu.memsub.promo.Renewal]]): Future[Renew] = Future {
      val newRatePlan = RatePlan(renewal.plan.id.get, None)
      val basicRenewCommand = Renew(
        subscriptionId = subscription.id.get,
        currentTermStartDate = subscription.termStartDate,
        currentTermEndDate = subscription.termEndDate,
        planToRemove = subscription.planToManage.id.get,
        newRatePlans = NonEmptyList(newRatePlan),
        newTermStartDate = newTermStartDate,
        promoCode = None,
        autoRenew = renewal.plan.charges.billingPeriod.isRecurring,
        fastForwardTermStartDate = currentVersionExpired // If the sub has expired, we need to shift the term dates forward
      )
      val finalRenewCommand = applyPromoIfNecessary(maybeValidPromo, basicRenewCommand)
      finalRenewCommand.withContextLogging("final renew command")
    }

    def getValidPromotion(contact: Contact): Option[ValidPromotion[com.gu.memsub.promo.Renewal]] = for {
        code <- renewal.promoCode.withContextLogging("promo code from client")
        deliveryCountryString <- contact.mailingCountry.withContextLogging("salesforce mailing country")
        deliveryCountry <- Some(Country.UK).withContextLogging("delivery country object")
        validPromotion <- promoService.validate[com.gu.memsub.promo.Renewal](code, deliveryCountry, renewal.plan.id).withContextLogging("validated promotion").toOption
    } yield validPromotion

    def applyPromoIfNecessary(maybeValidPromo: Option[ValidPromotion[com.gu.memsub.promo.Renewal]], basicRenewCommand: Renew): Renew = {
      maybeValidPromo match {
        case Some(validPromotion) => {
          info(s"Attempting to apply the promotion ${validPromotion.code}")
          p.apply(validPromotion, prpId => catalog.paid.find(_.id == prpId).map(_.charges.billingPeriod).get, promoPlans)(basicRenewCommand)
        }
        case None => {
          info(s"No promotion to apply, keeping basicRenewCommand")
          basicRenewCommand
        }
      }
    }

    def ensureEmail(contact: Contact, subscription: Subscription[WeeklyPlan]) =
      if (subscription.readerType != ReaderType.Direct || contact.email.isDefined) {
        info(s"email submitted on backend but not updated: ${subscription.readerType}, ${contact.email}")
        Future.successful(\/.right(()))
      } else {
        salesforceService.repo.addEmail(contact, renewal.email).map { either =>
          either.fold[Unit]({ err => error(s"couldn't update email for sub: ${subscription.id}: $err") }, u => u)
        }
      }

    def getSubscriptionPrice(maybeValidPromotion: Option[ValidPromotion[PromoContext]]): String = {
      val currency = subscription.currency
      val discountedPlanDescription = for {
        validPromo <- maybeValidPromotion
        discountPromotion <- validPromo.promotion.asDiscount
      } yield {
        renewal.plan.charges.prettyPricingForDiscountedPeriod(discountPromotion, currency)
      }
      val subscriptionDetails = discountedPlanDescription getOrElse renewal.plan.charges.prettyPricing(currency)
      subscriptionDetails.withContextLogging(s"Subscription price:")
    }

    def pricesMatch(displayedPrice: String, subscriptionPrice: String) = {
      if(displayedPrice != subscriptionPrice){
        logger.error(s"Client and server side prices divergent, we showed $displayedPrice and wanted to charge $subscriptionPrice from ${subscription.name} (${renewal.promoCode})")
        false
      }
      else true
    }

    for {
      account <- zuoraService.getAccount(subscription.accountId).withContextLogging("zuoraAccount", _.id)
      billto <- zuoraService.getContact(account.billToId).withContextLogging("zuora bill to", _.id)
      contact <- GetSalesforceContactForSub.sfContactForZuoraAccount(account)(zuoraService, salesforceService.repo, global).withContextLogging("sfContact", _.salesforceContactId)
      validPromotion = getValidPromotion(contact)
      subscriptionPrice = getSubscriptionPrice(validPromotion)
      if (pricesMatch(renewal.displayedPrice, subscriptionPrice))
      payment = getPayment(contact, billto)
      paymentMethod <- payment.makePaymentMethod
      createPaymentMethod = CreatePaymentMethod(subscription.accountId, paymentMethod, payment.makeAccount.paymentGateway, billto)
      updateResult <- zuoraService.createPaymentMethod(createPaymentMethod).withContextLogging("createPaymentMethod", _.id)
      renewCommand <- constructRenewCommand(validPromotion)
      amendResult <- zuoraService.renewSubscription(renewCommand)
      _ <- ensureEmail(contact, subscription)
      _ <- exactTargetService.enqueueRenewalEmail(subscription, renewal, subscriptionPrice, contact, renewal.email, newTermStartDate)
    }
      yield {
        updateResult
      }
  }
}
