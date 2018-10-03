package services

import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Country
import com.gu.i18n.Currency.GBP
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.memsub.Subscription.{AccountId, ProductRatePlanId}
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{ValidPromotion, _}
import com.gu.memsub.services.{GetSalesforceContactForSub, PromoService, PaymentService => CommonPaymentService}
import com.gu.memsub.subsv2.SubscriptionPlan.WeeklyPlan
import com.gu.memsub.subsv2.{Catalog, Subscription}
import com.gu.memsub.{BillingPeriod, NormalisedTelephoneNumber, Product}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.salesforce.{Contact, ContactId}
import com.gu.stripe.Stripe
import com.gu.zuora.rest.ZuoraRestService
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.{Account, PaymentMethod, RatePlan, Subscribe, _}
import com.gu.zuora.soap.models.Queries
import com.gu.zuora.soap.models.Queries.{Contact => ZuoraContact}
import com.gu.zuora.soap.models.Results.{AmendResult, SubscribeResult}
import com.gu.zuora.soap.models.errors._
import logging.{Context, ContextLogging}
import model.BillingPeriodOps._
import model.SubscriptionOps._
import model._
import model.error.CheckoutService._
import model.error.IdentityService._
import model.error.SubsError
import org.joda.time.LocalDate.now
import org.joda.time.{DateTimeConstants, Days, LocalDate}
import touchpoint.ZuoraProperties
import views.support.Pricing._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.{-\/, EitherT, Monad, NonEmptyList, \/, \/-}


object CheckoutService {
  def fulfilmentDelay(in: Either[PaperData, DigipackData])(implicit now: LocalDate): Days = in.fold(
    p => Days.daysBetween(now, p.startDate), d => Days.ZERO
  )
  def paymentDelay(in: Either[PaperData, DigipackData], zuora: ZuoraProperties)(implicit now: LocalDate): Days = in.fold(
    p => Days.daysBetween(now, p.startDate), d => zuora.gracePeriodInDays.plus(zuora.defaultDigitalPackFreeTrialPeriod)
  )


  def determineFirstAvailableWeeklyDate(now: LocalDate): LocalDate = {
    (now match {
      case d if d.getDayOfWeek < DateTimeConstants.THURSDAY => d.plusWeeks(1)
      case d => d.plusWeeks(2)
    }).withDayOfWeek(DateTimeConstants.FRIDAY)
  }
}

class CheckoutService(
  identityService: IdentityService[Future],
  salesforceService: SalesforceService,
  paymentService: PaymentService,
  catalog: Future[Catalog],
  zuoraService: ZuoraService,
  zuoraRestService: ZuoraRestService[Future],
  exactTargetService: ExactTargetService,
  zuoraProperties: ZuoraProperties,
  promoService: PromoService,
  promoPlans: DiscountRatePlanIds
)(implicit executionContext: ExecutionContext) extends ContextLogging {

  type NonFatalErrors = Seq[SubsError]
  type PostSubscribeResult = (Option[UserIdData], NonFatalErrors)
  type SubNel[A] = EitherT[Future, NonEmptyList[SubsError], A]
  type FatalErrors = NonEmptyList[SubsError]

  private def isGuardianWeekly(paperData: PaperData): Boolean = paperData.plan.product match {
    case _:Product.Weekly => true
    case _ => false
  }

  private val allSixWeekAssociations = catalog.map { catalog =>
    List(
      catalog.weekly.zoneA.associations,
      catalog.weekly.zoneC.associations,
      catalog.weekly.domestic.associations,
      catalog.weekly.restOfWorld.associations
    ).flatten
  }

  // This method is not genericised because the '6' is not stored in the association.
  def processSixWeekIntroductoryPeriod(daysUntilFirstIssue: Days, originalCommand: Subscribe): Future[Subscribe] = {
    allSixWeekAssociations.map { allSixWeekAssociations =>
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
  }

  def processSubscription(subscriptionData: SubscribeRequest,
                          authenticatedUserOpt: Option[AuthenticatedIdUser],
                          requestData: SubscriptionRequestData
                         )(implicit promotionApplicator: PromotionApplicator[NewUsers, Subscribe]): Future[NonEmptyList[SubsError] \/ CheckoutSuccess] = {

    import subscriptionData.genericData._
    val plan = RatePlan(subscriptionData.productRatePlanId.get, None)

    def emailError: SubNel[Unit] = EitherT(Future.successful(\/.left(NonEmptyList(CheckoutIdentityFailure("Email in use")))))

    val idMinimalUser = authenticatedUserOpt.map(_.user)

    val telephoneNumber = subscriptionData.productData match {
      case Left(paperData) => NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber, paperData.deliveryAddress.country orElse personalData.address.country)
      case _ => NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber, personalData.address.country)
    }

    val soldToContact = subscriptionData.productData match {
      case Left(paperData) => Some(SoldToContact(
        name = personalData, // TODO once we have gifting change this to the Giftee's name
        address = paperData.deliveryAddress,
        email = personalData.email, // TODO once we have gifting change this to the Giftee's email address
        phone = telephoneNumber,
        deliveryInstructions = paperData.sanitizedDeliveryInstructions
      ))
      case _ => None
    }

    implicit val today = now()
    import LogImplicit._

    (for {
      //check user exists
      userExists <- EitherT(identityService.doesUserExist(personalData.email).map(\/.right[FatalErrors, Boolean]))
      _ <- Monad[SubNel].whenM(userExists && authenticatedUserOpt.isEmpty)(emailError)
      //update salesforce contact
      contactId <- EitherT(createOrUpdateUserInSalesforce(subscriptionData, idMinimalUser))
      //Combine ID + SF ids
      combinedIds = PurchaserIdentifiers(contactId, idMinimalUser)
      //Prepare payment
      payment <- EitherT(createPaymentType(combinedIds, subscriptionData))
      paymentDelay = CheckoutService.paymentDelay(subscriptionData.productData, zuoraProperties)
      paymentMethod <- EitherT(attachPaymentMethodToStripeCustomer(payment, combinedIds))
      //prepare a sub to send to zuora
      fulfilmentDelay = CheckoutService.fulfilmentDelay(subscriptionData.productData)
      initialSubscribe = createSubscribeRequest(personalData, soldToContact, requestData, plan, combinedIds, paymentMethod, payment.makeAccount, Some(fulfilmentDelay), Some(paymentDelay), telephoneNumber)
      withTrial <- EitherT(processSixWeekIntroductoryPeriod(fulfilmentDelay, initialSubscribe).map(\/.right[FatalErrors, Subscribe]))
      //handle promotion
      validPromotion = promoCode.flatMap {
        promoService.validate[NewUsers](_, personalData.address.country.getOrElse(Country.UK), subscriptionData.productRatePlanId).toOption
      }.withLogging("validating promotion")
      withPromo <- EitherT(catalog.map { catalog =>
        validPromotion.map { v =>
          //TODO: This should just be type PlanFinder
          val planFinder: ProductRatePlanId => BillingPeriod = prpId => catalog.paid.find(_.id == prpId).map(_.charges.billingPeriod).get
          promotionApplicator.apply(v, planFinder, promoPlans)(withTrial)
        }.getOrElse(withTrial)
      }.map(\/.right[FatalErrors, Subscribe]))
      result <- EitherT(createSubscription(withPromo, combinedIds))
      out <- postSubscribeSteps(authenticatedUserOpt, contactId, result, subscriptionData, validPromotion)
    } yield CheckoutSuccess(contactId, out._1, result, validPromotion, out._2)).run
  }

  def postSubscribeSteps(user: Option[AuthenticatedIdUser],
                         contactId: ContactId,
                         result: SubscribeResult,
                         subscriptionData: SubscribeRequest,
                         promotion: Option[ValidPromotion[NewUsers]]): SubNel[PostSubscribeResult] = {

    val purchaserIds = PurchaserIdentifiers(contactId, user.map(_.user))

    val res = for {
      id <- storeIdentityDetails(subscriptionData, user, contactId, result).run
      _ <- sendConsentEmail(subscriptionData)
      email <- sendETDataExtensionRow(result, subscriptionData, gracePeriod(promotion), purchaserIds, promotion)
    } yield (id.toOption.map(_.userData), id.swap.toOption.toSeq.flatMap(_.list.toList) ++ email.swap.toOption.toSeq.flatMap(_.list.toList))

    EitherT(res.map(\/.right[FatalErrors, PostSubscribeResult]))
  }

  private def gracePeriod(promo: Option[ValidPromotion[NewUsers]]) =
    promo.flatMap(_.promotion.asDiscount).fold(zuoraProperties.gracePeriodInDays)(_ => Days.ZERO)

  private def sendConsentEmail(subscribeRequest: SubscribeRequest): Future[\/[NonEmptyList[IdentityFailure], Unit]] = {
    val personalData = subscribeRequest.genericData.personalData
    if (personalData.receiveGnmMarketing) {
      logger.info("User consented to marketing, sending consent email")
      identityService.consentEmail(personalData.email)
    }
    else {
      logger.info("User did not consent to marketing, skipping consent email")
      Future.successful(\/.right(Unit))
    }
  }


  private def storeIdentityDetails(
      subscribeRequest: SubscribeRequest,
      authenticatedUserOpt: Option[AuthenticatedIdUser],
      memberId: ContactId,
      result: SubscribeResult): EitherT[Future, NonEmptyList[SubsError], IdentitySuccess] = {

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
        EitherT(identityService.registerGuest(personalData, deliveryAddress)).leftMap(addErrContext("Guest")).map { identitySuccess =>
          val id = identitySuccess.userData.id.id
          EitherT(salesforceService.repo.updateIdentityId(memberId, id)).swap.map(err =>
            SafeLogger.error(scrub"Error updating salesforce contact ${memberId.salesforceContactId} with identity id $id: ${err.getMessage}")
          )
          EitherT(zuoraRestService.updateAccountIdentityId(AccountId(result.accountId), id)).swap.map(err =>
            SafeLogger.error(scrub"Error updating Zuora account ${result.accountId} with identity id $id: $err")
          )
          identitySuccess
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
        SafeLogger.error(scrub"$message: ${e.toString}")
        \/.left(NonEmptyList(CheckoutSalesforceFailure(
          userData,
          message)))
      }
    }
  }

  private def attachPaymentMethodToStripeCustomer(payment: PaymentService#AccountAndPayment, purchaserIds: PurchaserIdentifiers): Future[NonEmptyList[SubsError] \/ PaymentMethod] =
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
      maybeSoldToContact: Option[SoldToContact],
      requestData: SubscriptionRequestData,
      plan: RatePlan,
      purchaserIds: PurchaserIdentifiers,
      paymentMethod: PaymentMethod,
      acc: Account,
      fufilmentDelay: Option[Days],
      paymentDelay: Option[Days],
      telephoneNumber: Option[NormalisedTelephoneNumber]
                                    ): Subscribe = {

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
      soldToContact = maybeSoldToContact,
      contractEffective = acquisitionDate,
      contractAcceptance = firstPaymentDate,
      supplierCode = requestData.supplierCode,
      ipCountry = requestData.ipCountry,
      phone = telephoneNumber
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

  private def createPaymentType(purchaserIds: PurchaserIdentifiers, subscriptionData: SubscribeRequest): Future[NonEmptyList[SubsError] \/ PaymentService#AccountAndPayment] = {
    try {
      val personalData = subscriptionData.genericData.personalData

      val payment = subscriptionData.genericData.paymentData match {
        case paymentData@DirectDebitData(_, _, _) =>
          require(personalData.address.country.contains(Country.UK), "Direct Debit payment only works in the UK right now")
          paymentService.makeZuoraAccountWithDirectDebit(paymentData, personalData.first, personalData.last, purchaserIds)
        case paymentData@CreditCardData(_) =>
          val plan = subscriptionData.productData.fold(_.plan, _.plan)
          val desiredCurrency = subscriptionData.genericData.currency
          val currency = if (plan.charges.price.currencies.contains(desiredCurrency)) desiredCurrency else GBP
          paymentService.makeZuoraAccountWithCreditCard(paymentData, currency, purchaserIds, personalData.address.country)
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
    (implicit
      p: PromotionApplicator[com.gu.memsub.promo.Renewal, Renew],
      zuoraRestService: ZuoraRestService[Future],
      context: Context
    ): Future[\/[String, Any]] = {

    def getPayment(contact: Contact, billto: Queries.Contact): PaymentService#AccountAndPayment = {
      val idMinimalUser = IdMinimalUser(contact.identityId, None)
      val purchaserIds = PurchaserIdentifiers(contact, Some(idMinimalUser))
      renewal.paymentData match {
        case cd: CreditCardData => paymentService.makeZuoraAccountWithCreditCard(cd, subscription.currency, purchaserIds, billto.country)
        case dd: DirectDebitData => paymentService.makeZuoraAccountWithDirectDebit(dd, billto.firstName, billto.lastName, purchaserIds)
      }
    }

    val currentVersionExpired = subscription.termEndDate.isBefore(now).withContextLogging("currentVersionExpired")

    // For a renewal, all dates should be identical. If the sub has expired, this date should be fast-forwarded to the next available paper date
    val newTermStartDate = {
      if (currentVersionExpired) CheckoutService.determineFirstAvailableWeeklyDate(now) else subscription.termEndDate
    }.withContextLogging("startDateForRenewal")

    def constructRenewCommand(maybeValidPromo: Option[ValidPromotion[com.gu.memsub.promo.Renewal]]): Future[Renew] = {
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

    def getValidPromotion(code: PromoCode, addressCountry: Option[Country]): \/[String, ValidPromotion[com.gu.memsub.promo.Renewal]] = for {
      country <- addressCountry.withContextLogging("country client").toRightDisjunction("No country in contact!")
      validPromotion <- promoService.validate[com.gu.memsub.promo.Renewal](code, country, renewal.plan.id).leftMap(_.msg).withContextLogging("validated promotion")
    } yield validPromotion

    def applyPromoIfNecessary(maybeValidPromo: Option[ValidPromotion[com.gu.memsub.promo.Renewal]], basicRenewCommand: Renew): Future[Renew] = {
      catalog.map { catalog =>
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
    }

    def ensureEmail(contact: Queries.Contact, subscription: Subscription[WeeklyPlan]) = {
      if (contact.email.isDefined) {
        info(s"email submitted on backend but not updated: ${subscription.readerType}, ${contact.email}")
        Future.successful(\/.right(()))
      } else {
        zuoraRestService.addEmail(subscription.accountId, renewal.email).map { either =>
          either.fold[Unit]({ err => error(s"couldn't update email for sub: ${subscription.id}: $err") }, u => u)
        }
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

    case class ContactInfo(billto: ZuoraContact, soldto: ZuoraContact, salesforceContact: Contact)

    def executeRenewal(contactInfo: ContactInfo, promotion: Option[ValidPromotion[com.gu.memsub.promo.Renewal]]): Future[\/[String, AmendResult]] = {
      val subscriptionPrice = getSubscriptionPrice(promotion)
      if (renewal.displayedPrice != subscriptionPrice) {
        Future.successful(-\/(s"Client and server side prices divergent, we showed ${renewal.displayedPrice} and wanted to charge $subscriptionPrice from ${subscription.name} (${renewal.promoCode})"))
      }
      else {
        val payment = getPayment(contactInfo.salesforceContact, contactInfo.billto)
        val account = payment.makeAccount
        for {
          paymentMethod <- payment.makePaymentMethod
          createPaymentMethod = CreatePaymentMethod(subscription.accountId, paymentMethod, account.paymentGateway, contactInfo.billto, account.invoiceTemplate)
          updateResult <- zuoraService.createPaymentMethod(createPaymentMethod).withContextLogging("createPaymentMethod", _.id)
          renewCommand <- constructRenewCommand(promotion)
          _ <- ensureEmail(contactInfo.billto, subscription)
          amendResult <- zuoraService.renewSubscription(renewCommand)
          _ <- exactTargetService.enqueueRenewalEmail(subscription, renewal, subscriptionPrice, contactInfo.salesforceContact, newTermStartDate)
        }
          yield {
            \/-(amendResult)
          }
      }
    }

    val contactInfo: Future[ContactInfo] = for {
      account <- zuoraService.getAccount(subscription.accountId).withContextLogging("zuoraAccount", _.id)
      billto <- zuoraService.getContact(account.billToId).withContextLogging("zuora bill to", _.id)
      soldto <- zuoraService.getContact(account.soldToId).withContextLogging("zuora sold to", _.id)
      contact <- GetSalesforceContactForSub.sfContactForZuoraAccount(account)(zuoraService, salesforceService.repo, implicitly).withContextLogging("sfContact", _.salesforceContactId)
    }
      yield (ContactInfo(billto, soldto, contact))

    contactInfo.flatMap { info =>
      renewal.promoCode.withContextLogging("promo code from client") match {
        case None => executeRenewal(info, None)
        case Some(promoCode) => {
          getValidPromotion(promoCode, info.soldto.country.orElse(info.billto.country)) match {
            case \/-(promotion) => executeRenewal(info, Some(promotion))
            case error => Future.successful(error)
          }
        }
      }
    }
  }
}
