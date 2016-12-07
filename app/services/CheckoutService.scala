package services

import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Country
import com.gu.i18n.Currency.GBP
import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.memsub.promo._
import com.gu.memsub.services.{GetSalesforceContactForSub, PromoService}
import com.gu.memsub.subsv2.{Catalog, Subscription, SubscriptionPlan}
import com.gu.memsub.{Address, Product}
import com.gu.salesforce.{Contact, ContactId}
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Commands.{Account, PaymentMethod, RatePlan, Subscribe, _}
import com.gu.zuora.soap.models.Queries
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import model.{Renewal, _}
import model.error.CheckoutService._
import model.error.IdentityService._
import model.error.SubsError
import org.joda.time.{DateTimeConstants, Days, LocalDate}
import touchpoint.ZuoraProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.{EitherT, Monad, NonEmptyList, \/}


object CheckoutService {
  def paymentDelay(in: Either[PaperData, DigipackData], zuora: ZuoraProperties)(implicit now: LocalDate): Days = in.fold(
    p => Days.daysBetween(now, p.startDate), d => zuora.gracePeriodInDays.plus(zuora.paymentDelayInDays)
  )
}

class CheckoutService(identityService: IdentityService,
                      salesforceService: SalesforceService,
                      paymentService: PaymentService,
                      catalog: Catalog,
                      zuoraService: ZuoraService,
                      exactTargetService: ExactTargetService,
                      zuoraProperties: ZuoraProperties,
                      promoService: PromoService,
                      promoPlans: DiscountRatePlanIds) extends LazyLogging {

  type NonFatalErrors = Seq[SubsError]
  type PostSubscribeResult = (Option[UserIdData], NonFatalErrors)
  type SubNel[A] = EitherT[Future, NonEmptyList[SubsError], A]
  type FatalErrors = NonEmptyList[SubsError]

  private def isGuardianWeekly(paperData: PaperData): Boolean = {
    paperData.plan.product == Product.WeeklyZoneA || paperData.plan.product == Product.WeeklyZoneB
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

    implicit val today = LocalDate.now()

    (for {
      userExists <- EitherT(IdentityService.doesUserExist(personalData.email).map(\/.right[FatalErrors, Boolean]))
      _ <- Monad[SubNel].whenM(userExists && authenticatedUserOpt.isEmpty)(emailError)
      memberId <- EitherT(createOrUpdateUserInSalesforce(subscriptionData, idMinimalUser))
      purchaserIds = PurchaserIdentifiers(memberId, idMinimalUser)
      payment <- EitherT(createPaymentType(purchaserIds, subscriptionData))
      defaultPaymentDelay = CheckoutService.paymentDelay(subscriptionData.productData, zuoraProperties)
      paymentMethod <- EitherT(attachPaymentMethodToStripeCustomer(payment, purchaserIds))
      subscribe = createSubscribeRequest(personalData, soldToContact, requestData, plan, purchaserIds, paymentMethod, payment.makeAccount, Some(defaultPaymentDelay))
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
      case e => \/.left(NonEmptyList(CheckoutSalesforceFailure(
        userData,
        s"${userData} could not subscribe during checkout because his details could not be updated in Salesforce")))
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
      paymentDelay: Option[Days]): Subscribe =

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
      paymentDelay = paymentDelay,
      ipAddress = requestData.ipAddress.map(_.getHostAddress),
      supplierCode = requestData.supplierCode,
      ipCountry = requestData.ipCountry
    )

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


  private def nextFridayFrom(d: LocalDate) = {
    val nearestFriday = d.withDayOfWeek(DateTimeConstants.FRIDAY)
    if (!nearestFriday.isBefore(d)) {
      nearestFriday
    } else {
      d.plusWeeks(1).withDayOfWeek(DateTimeConstants.FRIDAY)
    }
  }

  def renewSubscription(subscription: Subscription[SubscriptionPlan.PaperPlan], renewal: Renewal ) = {

    def getPayment(contact: Contact, billto: Queries.Contact): PaymentService#Payment = {
      val idMinimalUser = IdMinimalUser(contact.identityId, None)
      val pid = PurchaserIdentifiers(contact, Some(idMinimalUser))
      renewal.paymentData match {
        case cd: CreditCardData =>
          val currency = subscription.plan.charges.currencies.head
          paymentService.makeCreditCardPayment(cd, currency, pid)
        case dd: DirectDebitData => paymentService.makeDirectDebitPayment(dd, billto.firstName, billto.lastName, contact)
      }
    }

    val ratePlan = RatePlan(renewal.plan.id.get, None, Nil)
    val amend =   Amend(subscriptionId = subscription.id.get, plansToRemove = Nil, newRatePlans = NonEmptyList(ratePlan))

    def addPlan = {

      //TODO I think we are supposed to use the next friday date for everything but that causes a zuora error : "contract effective date should not be later than the term end date of the sub"
      val contractEffective = subscription.plan.end
      val customerAcceptance = nextFridayFrom(contractEffective)

      val newRatePlan = RatePlan(renewal.plan.id.get, None)

      val renewCommand =  Renew(subscription.id.get, newRatePlan, contractEffective, customerAcceptance)
      zuoraService.renewSubscription(renewCommand)
    }

    for {
      account <- zuoraService.getAccount(subscription.accountId)
      billto <- zuoraService.getContact(account.billToId)
      contact <- GetSalesforceContactForSub(subscription)(zuoraService, salesforceService.repo, global)
      payment = getPayment(contact, billto)
      paymentMethod <- payment.makePaymentMethod
      createPaymentMethod = CreatePaymentMethod(subscription.accountId, paymentMethod, payment.makeAccount.paymentGateway, billto)
      updateResult <- zuoraService.createPaymentMethod(createPaymentMethod)
      amendResult <- addPlan
    }
      yield {
        updateResult
      }
  }
}
