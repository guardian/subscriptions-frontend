package controllers

import _root_.services.AuthenticationService._
import _root_.services.TouchpointBackends.Resolution
import _root_.services.{LookupSubscriptionFulfilment, TouchpointBackend, TouchpointBackends}
import actions.CommonActions
import com.gu.i18n.Country.{UK, US}
import com.gu.i18n.Currency
import com.gu.i18n.Currency.{GBP, USD}
import com.gu.memsub.Subscription.{Name, ProductRatePlanId}
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.memsub.subsv2.SubscriptionPlan._
import com.gu.memsub.subsv2.{Catalog, ReaderType, Subscription}
import com.gu.memsub.{BillingSchedule, PaymentCard, Product}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.subscriptions.suspendresume.SuspensionService.{BadZuoraJson, ErrNel, HolidayRefund, PaymentHoliday}
import com.gu.zuora.rest.ZuoraRestService
import com.gu.zuora.soap.models.Queries.{Account, Contact}
import com.typesafe.scalalogging.StrictLogging
import configuration.{Config, ProfileLinks}
import forms._
import logging.{Context, ContextLogging}
import model.ContentSubscriptionPlanOps._
import model.SubscriptionOps._
import model.{Renewal, RenewalReads}
import okhttp3.{Response, Request => OKRequest}
import org.joda.time.LocalDate.now
import play.api.libs.json._
import play.api.mvc.{AnyContent, _}
import utils.TestUsers.PreSigninTestCookie
import views.html.account.thankYouRenew
import views.support.Dates._
import views.support.Pricing._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.{-\/, EitherT, NonEmptyList, OptionT, \/, \/-}

// this handles putting subscriptions in and out of the session
object SessionSubscription extends StrictLogging {

  val SUBSCRIPTION_SESSION_KEY = "subscriptionId"

  def set(result: Result, sub: Subscription[ContentSubscription]): Result =
    result.withSession(
      SUBSCRIPTION_SESSION_KEY -> sub.name.get
    )

  def clear(result: Result)(implicit request: Request[AnyContent]): Result =
    result.withSession(request.session - SUBSCRIPTION_SESSION_KEY)

  def subscriptionFromRequest(implicit request: Request[_], tpBackend: TouchpointBackend, executionContext: ExecutionContext): Future[Option[Subscription[ContentSubscription]]] = {

    (for {
      subscriptionId <- OptionT(Future.successful(request.session.data.get(SUBSCRIPTION_SESSION_KEY)))
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.get[ContentSubscription](Name(subscriptionId)))
    } yield zuoraSubscription).orElse(for {
      identityUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      /* TODO: Use a Zuora-only based lookup from an Identity ID. This needs code pulling up from Members Data API into Membership Common */
      salesForceUser <- OptionT(tpBackend.salesforceService.repo.get(identityUser.user.id).map { d =>
        d.leftMap(e => logger.warn(s"Error looking up SF Contact for logged in user with Identity ID ${identityUser.user.id}: $e")).toOption.flatten
      })
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.current[ContentSubscription](salesForceUser).map{subs =>
        if (subs.length > 1) logger.warn(s"Logged in user with Identity ID ${identityUser.user.id}: with ${subs.length} subscriptions, only serving first.")
        subs.headOption
        /*FIXME if they have more than one they can only manage the first*/})
    } yield zuoraSubscription).run
  }

}

class ManageDelivery(implicit val executionContext: ExecutionContext) extends ContextLogging {

  import play.api.mvc.Results._

  def apply(errorCodes: Set[String], pendingHolidays: Seq[HolidayRefund], billingSchedule: Option[BillingSchedule], deliverySubscription: Subscription[Delivery], account: Account, maybeEmail: Option[String], paymentMethodIsPaymentCard: Boolean)(implicit request: Request[AnyContent], touchpoint: TouchpointBackends.Resolution): Result = {
    val suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, deliverySubscription.plan.charges.chargedDays.toList)
    val chosenPaperDays = deliverySubscription.plan.charges.chargedDays.toList.sortBy(_.dayOfTheWeekIndex)
    val suspendableDays = Config.suspendableWeeks * chosenPaperDays.size
    Ok(views.html.account.delivery(deliverySubscription, account, pendingHolidays, billingSchedule, chosenPaperDays, suspendableDays, suspendedDays, errorCodes, maybeEmail, paymentMethodIsPaymentCard))
  }

  def suspend(implicit request: Request[AnyContent], touchpoint: TouchpointBackends.Resolution): Future[Result] = {
    implicit val tpBackend = touchpoint.backend
    implicit val rest = tpBackend.simpleRestClient
    implicit val zuoraRestService = new ZuoraRestService[Future]

    (for {
      form <- EitherT(Future.successful(SuspendForm.mappings.bindFromRequest().value \/> "Please check your selections and try again"))
      maybeDeliverySub <- EitherT(SessionSubscription.subscriptionFromRequest.map(_ \/> "Could not find an active subscription"))
      sub <- EitherT(Future.successful(maybeDeliverySub.asDelivery \/> "Is not a Home Delivery subscription"))
      account <- EitherT(recoverToDisjunction(tpBackend.zuoraService.getAccount(sub.accountId), "Unable to retrieve account details"))
      newHoliday = PaymentHoliday(sub.name, form.startDate, form.endDate)
      _ <- EitherT(tpBackend.suspensionService.renewIfNeeded(sub,newHoliday))
      // 26 because one year from now could be end of second years sub + 2 extra months needed in calculation to cover setting a 6-week suspension on the day before your 12th billing day!
      oldBS <- EitherT(tpBackend.commonPaymentService.flatMap(_.billingSchedule(sub.id, account, numberOfBills = 26).map(_ \/> "Error getting billing schedule")))
      result <- EitherT(tpBackend.suspensionService.addHoliday(newHoliday, oldBS, account.billCycleDay, sub.termEndDate)).leftMap(getAndLogRefundError(_).map(_.code).list.toList.mkString(","))
      newBS <- EitherT(tpBackend.commonPaymentService.flatMap(_.billingSchedule(sub.id, account, numberOfBills = 24).map(_ \/> "Error getting billing schedule")))
      pendingHolidays <- EitherT(tpBackend.suspensionService.getUnfinishedHolidays(sub.name, now)).leftMap(_ => "Error getting holidays")
      suspendableDays = Config.suspendableWeeks * sub.plan.charges.chargedDays.size
      suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, sub.plan.charges.chargedDays.toList)
    } yield {
      tpBackend.exactTargetService.enqueueETHolidaySuspensionEmail(sub, sub.plan.name, newBS, pendingHolidays.size, suspendableDays, suspendedDays).onFailure { case e: Throwable =>
        error(s"Failed to generate data needed to enqueue ${sub.name.get}'s holiday suspension email. Reason: ${e.getMessage}")(sub)
      }
      Ok(views.html.account.suspensionSuccess(
        newRefund = result.refund -> newHoliday,
        holidayRefunds = pendingHolidays,
        subscription = sub,
        billingSchedule = newBS,
        suspendableDays = suspendableDays,
        suspendedDays = suspendedDays,
        currency = sub.currency
      ))
    }).valueOr(errorCode => Redirect(routes.AccountManagement.manage(None, Some(errorCode), None).url))
  }

  /**
    * Takes a RefundError error inside a NonEmptyList and prints each error line to the log as a side effect, returning the original NonEmptyList.
    *
    * @param r a NonEmptyList of RefundErrors
    * @return NonEmptyList[RefundError]
    */
  private def getAndLogRefundError(r: ErrNel): ErrNel = {
    r.foreach {
      case e:BadZuoraJson => SafeLogger.error(scrub"Error when adding a new holiday - BadZuoraJson: ${e.got}")
      case e => SafeLogger.error(scrub"Error when adding a new holiday: ${e.code}")
    }
    r
  }

  private def recoverToDisjunction[A](eventualA: Future[A], replacementErrorMessage: String): Future[\/[String, A]] = {
    eventualA.map(\/-.apply).recover {
      case t: Throwable =>
        logger.error(t.toString)
        \/.left(replacementErrorMessage)
    }
  }

  def fulfilmentLookup(httpClient: OKRequest => Future[Response])(implicit request: Request[ReportDeliveryProblem], touchpoint: TouchpointBackends.Resolution): Future[Result] = {
    val env = touchpoint.backend.environmentName
    val deliveryProblem = request.body
    logger.info(s"[${env}] Attempting to raise a delivery issue: $deliveryProblem")
    val futureLookupAttempt = LookupSubscriptionFulfilment(env, httpClient, deliveryProblem)
    futureLookupAttempt.map { lookupAttempt => lookupAttempt match {
        case \/-(_) =>
          logger.info(s"[${env}] Successfully raised a delivery issue for $deliveryProblem")
          Ok(views.html.account.reportDeliveryProblemSuccess())
        case -\/(message) =>
          SafeLogger.error(scrub"[${env}] Failed to raise a delivery issue for ${deliveryProblem.subscriptionName}: $message")
          Ok(views.html.account.reportDeliveryProblemFailure())
      }
    }
  }

}

object ManageWeekly extends ContextLogging {

  // this sequencing concatenates errors if any, otherwise aggregates rights
  def sequence[A](list: List[\/[NonEmptyList[String], A]]): \/[NonEmptyList[String], List[A]] = {
    val errors = (list collect {
      case -\/(x) => x.list.toList
    }).flatten
    errors match {
      case head :: tail =>
        -\/(NonEmptyList(head, tail: _*))
      case Nil =>
        \/-(list collect {
          case \/-(x) => x
        })
    }
  }

  case class WeeklyPlanInfo(id: ProductRatePlanId, price: String)

  object WeeklyPlanInfo {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    implicit def writer: Writes[WeeklyPlanInfo] =
      (
        (JsPath \ "id").write[String].contramap[ProductRatePlanId](_.get) and
          (JsPath \ "price").write[String]
        ) (unlift(ManageWeekly.WeeklyPlanInfo.unapply))

  }

}

class ManageWeekly(implicit val executionContext: ExecutionContext) extends ContextLogging {

  import ManageWeekly._
  import play.api.mvc.Results._

  def apply(
             billingSchedule: Option[BillingSchedule],
             weeklySubscription: Subscription[WeeklyPlan],
             maybeEmail: Option[String],
             promoCode: Option[PromoCode],
             paymentMethodIsPaymentCard: Boolean
           )(implicit
             request: Request[AnyContent],
             resolution: TouchpointBackends.Resolution
           ): Future[Result] = {
    implicit val tpBackend = resolution.backend
    implicit val rest = tpBackend.simpleRestClient
    implicit val zuoraRest = new ZuoraRestService[Future]
    implicit val flash = request.flash
    implicit val subContext = weeklySubscription

    def getRenewalPlans(account: ZuoraRestService.AccountSummary, currency: Currency): Future[\/[NonEmptyList[String], List[WeeklyPlanInfo]]] = {
      EitherT(tpBackend.catalogService.catalog).flatMap { catalog =>

        // Identifies customers who have been wrongly migrated in on a price-adjusted Zone B or C rate plan and are
        // having the paper delivered to the UK or USA, and pay by GBP or USD respectively. These customers should
        // be offered Zone A rate plans upon renewal.
        def shouldBeInZoneA = account.soldToContact.country.exists { soldToCountry =>
          (soldToCountry == UK && currency == GBP) || (soldToCountry == US && currency == USD)
        }

        val weeklyPlans = if (shouldBeInZoneA) catalog.weekly.zoneA.plans else catalog.weekly.zoneB.plans

        val renewalPlans = weeklyPlans.filter(_.availableForRenewal)

        val renewalPlansInfo: \/[NonEmptyList[String], List[WeeklyPlanInfo]] = sequence(renewalPlans.map { plan =>
          val price = plan.charges.price.getPrice(currency).toRightDisjunction(NonEmptyList(s"could not find price in $currency for plan ${plan.id} ${plan.name}"))
          price.map(price => WeeklyPlanInfo(plan.id, plan.charges.prettyPricing(price.currency)))
        })

        EitherT(Future.successful(renewalPlansInfo): Future[\/[NonEmptyList[String], List[WeeklyPlanInfo]]])
      }.run
    }

    def choosePage(account: ZuoraRestService.AccountSummary): Future[\/[String, Result]] = {
      val renewPageResult = for {
        billToCountry <- EitherT(Future.successful(account.billToContact.country.toRightDisjunction(s"no valid bill to country for account ${account.id}")))
        currency <- EitherT(Future.successful(account.currency.toRightDisjunction(s"couldn't get new rate/currency for renewal ${account.id}")))
        weeklyPlanInfo <- EitherT(getRenewalPlans(account, currency).map(_.leftMap(errorMessage => s"couldn't get new rate: $errorMessage")))
      } yield {
        Ok(weeklySubscription.asRenewable.map { renewableSub =>
          info(s"sub is renewable - showing weeklyRenew page")
          views.html.account.weeklyRenew(renewableSub, account.soldToContact, account.billToContact.email, billToCountry, weeklyPlanInfo, currency, promoCode)
        } getOrElse {
          info(s"sub is not renewable - showing weeklyDetails page")
          views.html.account.weeklyDetails(weeklySubscription, billingSchedule, account.soldToContact, maybeEmail, paymentMethodIsPaymentCard, Some(billToCountry))
        })
      }
      renewPageResult.run
    }

    def maybePageToShow = {
      (for {
        account <- EitherT(zuoraRest.getAccount(weeklySubscription.accountId))
        page <- EitherT(choosePage(account))
      } yield page).run
    }

    if (weeklySubscription.readerType == ReaderType.Agent) {
      info(s"don't support agents, can't manage sub")
      Future.successful(Ok(views.html.account.details(None, promoCode, Some("You subscribe via an agent, at present you can't manage it via the web, please contact Customer Services for help.")))) // don't support gifts (yet) as they have related contacts in salesforce of unknown structure
    } else {
      maybePageToShow.map(_.leftMap(errorMessage => {
        error(s"problem getting account: $errorMessage")
        Ok(views.html.account.details(None, promoCode, Some("We found your subscription, but it can't be managed via the web, please contact Customer Services for help.")))
      }).fold(identity, identity))
    }
  }

  def renew(implicit request: Request[AnyContent], touchpoint: TouchpointBackends.Resolution): Future[Result] = {
    implicit val tpBackend = touchpoint.backend
    implicit val rest = tpBackend.simpleRestClient
    implicit val zuoraRestService = new ZuoraRestService[Future]

    def jsonError(message: String) = Json.toJson(Json.obj("errorMessage" -> message))

    def returnError(errorMessage: String, context: Context) = {
      val fullError = s"Unexpected error while renewing subscription : $errorMessage"
      error(fullError)(context)
      InternalServerError(jsonError(fullError))
    }

    SessionSubscription.subscriptionFromRequest flatMap { maybeSub: Option[Subscription[ContentSubscription]] =>
      val response = for {
        sub <- EitherT[Future, String, Subscription[ContentSubscription]](Future.successful(maybeSub.toRightDisjunction("no subscription in request")))
        weeklySub <- EitherT(Future.successful(sub.asWeekly.toRightDisjunction("subscription is not weekly")))
        renewableSub <- EitherT(Future.successful(weeklySub.asRenewable(sub).toRightDisjunction("subscription is not renewable")))
        catalog <- EitherT(tpBackend.catalogService.catalog.map(_.leftMap(_.list.toList.mkString(", "))))
        renew <- EitherT(Future.successful(parseRenewalRequest(request, catalog)))
      } yield {
        info(s"Attempting to renew onto ${renew.plan.name} with promo code: ${renew.promoCode}")(sub)
        tpBackend.checkoutService.renewSubscription(renewableSub, renew)(implicitly, implicitly, sub).map {
          case \/-(_) =>
            info(s"Successfully processed renewal onto ${renew.plan.name}")(sub)
            Ok(Json.obj("redirect" -> routes.AccountManagement.renewThankYou().url))
          case -\/(error) => returnError(error, sub)
        }.recover {
          case e: Throwable =>
            returnError(e.getMessage, sub)
        }
      }
      response.run.map(_.valueOr(error => Future(BadRequest(jsonError(error))))).flatMap(identity)
    }
  }

  def renewThankYou(implicit request: Request[AnyContent], touchpoint: TouchpointBackends.Resolution): Future[Result] = {
    implicit val tpBackend = touchpoint.backend

    import model.SubscriptionOps._

    val res = for {
      subscription <- OptionT(SessionSubscription.subscriptionFromRequest)
      billingSchedule <- OptionT(tpBackend.commonPaymentService.flatMap(_.billingSchedule(subscription.id, subscription.accountId, numberOfBills = 13).map(Some(_):Option[Option[BillingSchedule]])))
    } yield {
      Ok(thankYouRenew(subscription.nextPlan, billingSchedule, touchpoint))
    }
    res.run.map(_.getOrElse(Redirect(routes.Homepage.index()).withNewSession))
  }

  private def parseRenewalRequest(request: Request[AnyContent], catalog: Catalog): \/[String, Renewal] = {
    implicit val renewalReads = new RenewalReads(catalog).renewalReads
    request.body.asJson.map(_.validate[Renewal]) match {
      case Some(JsSuccess(renewal, _)) => \/-(renewal)
      case Some(JsError(err)) => -\/(err.mkString(","))
      case None => -\/("invalid json")
    }
  }
}


class AccountManagement(
  touchpointBackends: TouchpointBackends,
  commonActions: CommonActions,
  httpClient: OKRequest => Future[Response],
  override protected val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext) extends BaseController with ContextLogging {

  import commonActions.NoCacheAction

  val accountManagementAction = NoCacheAction
  val manageWeekly: ManageWeekly = new ManageWeekly()
  val manageDelivery: ManageDelivery = new ManageDelivery()

  def subscriptionFromUserDetails(loginRequestOpt: Option[AccountManagementLoginRequest])(implicit request: Request[AnyContent]): Future[Option[Subscription[ContentSubscription]]] = {
    implicit val resolution: TouchpointBackends.Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def detailsMatch(zuoraContact: Contact, loginRequest: AccountManagementLoginRequest): Boolean = {
      def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase
      format(zuoraContact.lastName) == format(loginRequest.lastname)
    }

    def subscriptionDetailsMatch(loginRequest: AccountManagementLoginRequest, zuoraSubscription: Subscription[ContentSubscription]): Future[Boolean] = {
      for {
        zuoraAccount <- tpBackend.zuoraService.getAccount(zuoraSubscription.accountId)
        zuoraContact <- tpBackend.zuoraService.getContact(zuoraAccount.billToId)
      } yield detailsMatch(zuoraContact, loginRequest)
    }

    loginRequestOpt.map { loginRequest =>
      (for {
        zuoraSubscription <- OptionT(tpBackend.subscriptionService.get[ContentSubscription](Name(loginRequest.subscriptionId)))
        result <- OptionT(subscriptionDetailsMatch(loginRequest, zuoraSubscription).map(matches => if (matches) Some(zuoraSubscription) else None))
      } yield result).run
    }.getOrElse(Future.successful(None))

  }

  def manage(subscriberId: Option[String] = None, errorCode: Option[String] = None, promoCode: Option[PromoCode] = None): Action[AnyContent] = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val eventualMaybeSubscription = SessionSubscription.subscriptionFromRequest
    val errorCodes = errorCode.toSeq.flatMap(_.split(',').map(_.trim)).filterNot(_.isEmpty).toSet

    val futureMaybeEmail: OptionT[Future, String] = for {
      authUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      idUser <- OptionT(touchpointBackends.identityService.userLookupByCredentials(authUser.credentials))
    } yield idUser.primaryEmailAddress

    val futureSomeMaybeEmail: Future[Option[Option[String]]] =  futureMaybeEmail.run.map(a => Some(a))

    val futureMaybeFutureManagePage = for {
      subscription <- OptionT(eventualMaybeSubscription).filter(!_.isCancelled)
      account <- OptionT(tpBackend.zuoraService.getAccount(subscription.accountId).map(Option(_)))
      billToContact <- OptionT(tpBackend.zuoraService.getContact(account.billToId).map(Option(_)))
      pendingHolidays <- OptionT(tpBackend.suspensionService.getUnfinishedHolidays(subscription.name, now).map(_.toOption))
      billingSchedule <- OptionT(tpBackend.commonPaymentService.flatMap(_.billingSchedule(subscription.id, account, numberOfBills = 13).map(Option(_))))
      maybeEmail <- OptionT(futureSomeMaybeEmail)
      maybePaymentMethod <- OptionT(tpBackend.commonPaymentService.flatMap(_.getPaymentMethod(subscription.accountId).map(Option(_))))
    } yield {
      val paymentMethodIsPaymentCard = maybePaymentMethod.collect { case _:PaymentCard => true }.isDefined
      val maybeFutureManagePage = subscription.planToManage.product match {
        case Product.Delivery => subscription.asDelivery.map { deliverySubscription =>
          Future.successful(manageDelivery(errorCodes, pendingHolidays, billingSchedule, deliverySubscription, account, maybeEmail, paymentMethodIsPaymentCard))
        }
        case Product.Voucher => subscription.asVoucher.map { voucherSubscription =>
          Future.successful(Ok(views.html.account.voucher(voucherSubscription, billingSchedule, maybeEmail, paymentMethodIsPaymentCard)))
        }
        case _: Product.Weekly => subscription.asWeekly.map { weeklySubscription =>
          manageWeekly(billingSchedule, weeklySubscription, maybeEmail, promoCode, paymentMethodIsPaymentCard)
        }
        case Product.Digipack => subscription.asDigipack.map { digipackSubscription =>
          Future.successful(Ok(views.html.account.digitalpack(digipackSubscription, billingSchedule, billToContact.country, maybeEmail, paymentMethodIsPaymentCard)))
        }
      }
      maybeFutureManagePage.getOrElse {
        // the product type didn't have the right charges
        Future.successful(Ok(views.html.account.details(None, promoCode, Some("We found your subscription, but can't manage it via the web, please contact Customer Services for help."))))
      }
    }

    futureMaybeFutureManagePage.getOrElse {
      // not a valid AS number or some unnamed problem getting the details or no sub details in the session yet
      Future.successful(Ok(views.html.account.details(subscriberId, promoCode)))
    }.flatMap(identity)
  }

  def logout: Action[AnyContent] = accountManagementAction { implicit request =>
    SessionSubscription.clear(Redirect(ProfileLinks.signOut.href, SEE_OTHER))
  }

  def processLogin: Action[AnyContent] = accountManagementAction.async { implicit request =>
    val loginRequest = AccountManagementLoginForm.mappings.bindFromRequest().value
    val promoCode = loginRequest.flatMap(_.promoCode).map(NormalisedPromoCode.safeFromString)
    def loginError(errorMessage: String) = Redirect(routes.AccountManagement.manage(None, None, promoCode)).flashing(
      "error" -> errorMessage
    )
    subscriptionFromUserDetails(loginRequest).map {
        case Some(sub) if (sub.isCancelled) =>  loginError(s"Your subscription is cancelled as of ${sub.termEndDate.pretty}, please contact Customer Services.")
        case Some(sub) => SessionSubscription.set(Redirect(routes.AccountManagement.manage(None, None, promoCode)), sub)
        case _ => loginError("Unable to verify your details.")
    }
  }

  def processSuspension: Action[AnyContent] = accountManagementAction.async { implicit request =>
    implicit val resolution: Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    manageDelivery.suspend
  }

  def redirect = NoCacheAction { implicit request =>
    Redirect(routes.AccountManagement.manage(None, None, None).url)
  }

  def processRenewal: Action[AnyContent] = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    manageWeekly.renew
  }

  def renewThankYou: Action[AnyContent] = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    manageWeekly.renewThankYou
  }

  def reportDeliveryProblem: Action[ReportDeliveryProblem] = accountManagementAction.async(parse.form(ReportDeliveryProblemForm.report)) { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = touchpointBackends.forRequest(PreSigninTestCookie, request.cookies)
    manageDelivery.fulfilmentLookup(httpClient)
  }

}
