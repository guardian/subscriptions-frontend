package controllers

import actions.CommonActions._
import com.github.nscala_time.time.OrderingImplicits.LocalDateOrdering
import com.gu.memsub.{Weekly, BillingPeriod, Product}
import com.gu.memsub.Subscription.Name
import com.gu.memsub.subsv2._
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.subscriptions.suspendresume.SuspensionService.{BadZuoraJson, ErrNel, HolidayRefund, PaymentHoliday}
import com.gu.zuora.soap.models.Queries.Contact
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, ProfileLinks}
import forms.{AccountManagementLoginForm, AccountManagementLoginRequest, SuspendForm}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Controller, Request}
import _root_.services.AuthenticationService._
import _root_.services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.{EitherT, OptionT, \/}

object AccountManagement extends Controller with LazyLogging {

  val accountManagementAction = NoCacheAction

  val SUBSCRIPTION_SESSION_KEY = "subscriptionId"

  @deprecated("use the other one and pattern match on the plan")
  private def subscriptionFromRequestOld(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.Delivery] \/ Subscription[SubscriptionPlan.Voucher]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      subscriptionId <- OptionT(Future.successful(request.session.data.get(SUBSCRIPTION_SESSION_KEY)))
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.either[SubscriptionPlan.Delivery, SubscriptionPlan.Voucher](Name(subscriptionId)))
    } yield zuoraSubscription).orElse(for {
      identityUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      salesForceUser <- OptionT(tpBackend.salesforceService.repo.get(identityUser.user.id))
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.either[SubscriptionPlan.Delivery, SubscriptionPlan.Voucher](salesForceUser))
    } yield zuoraSubscription).run
  }

  private def subscriptionFromRequest(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.PaperPlan]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      subscriptionId <- OptionT(Future.successful(request.session.data.get(SUBSCRIPTION_SESSION_KEY)))
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.get[SubscriptionPlan.PaperPlan](Name(subscriptionId)))
    } yield zuoraSubscription).orElse(for {
      identityUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      salesForceUser <- OptionT(tpBackend.salesforceService.repo.get(identityUser.user.id))
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.current[SubscriptionPlan.PaperPlan](salesForceUser).map(_.headOption))
    } yield zuoraSubscription).run
  }

  private def subscriptionFromUserDetails(loginRequestOpt: Option[AccountManagementLoginRequest])(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.PaperPlan]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def detailsMatch(zuoraContact: Contact, loginRequest: AccountManagementLoginRequest): Boolean = {
      def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase

      println(s"$zuoraContact")

      format(zuoraContact.lastName) == format(loginRequest.lastname) &&
        zuoraContact.postalCode.map(format).contains(format(loginRequest.postcode))
    }

    def subscriptionDetailsMatch(loginRequest: AccountManagementLoginRequest, zuoraSubscription: Subscription[SubscriptionPlan.PaperPlan]): Future[Boolean] = {
      for {
        zuoraAccount <- tpBackend.zuoraService.getAccount(zuoraSubscription.accountId)
        zuoraContact <- tpBackend.zuoraService.getContact(zuoraAccount.billToId)
      } yield detailsMatch(zuoraContact, loginRequest)
    }

    loginRequestOpt.map { loginRequest =>
      (for {
        zuoraSubscription <- OptionT(tpBackend.subscriptionService.get[SubscriptionPlan.PaperPlan](Name(loginRequest.subscriptionId)))
        result <- OptionT(subscriptionDetailsMatch(loginRequest, zuoraSubscription).map(matches => if (matches) Some(zuoraSubscription) else None))
      } yield result).run
    }.getOrElse(Future.successful(None))

  }

  private def pendingHolidayRefunds(allHolidays: Seq[HolidayRefund]) = allHolidays.filterNot(_._2.finish.isBefore(LocalDate.now)).sortBy(_._2.start)

  def login(subscriberId: Option[String] = None, errorCode: Option[String] = None) = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val subscription = subscriptionFromRequest
    val errorCodes = errorCode.toSeq.flatMap(_.split(',').map(_.trim)).filterNot(_.isEmpty).toSet

    object SubMatch {
      def unapply[A <: Product.Paper, B <: PaidChargeList](sub: Subscription[PaidSubscriptionPlan[A, B]]): Option[(A, B, Subscription[PaidSubscriptionPlan[A, B]])] =
        Some((sub.plan.product, sub.plan.charges, sub))
    }

    (for {
      sub <- OptionT(subscription)
      allHolidays <- OptionT(tpBackend.suspensionService.getHolidays(sub.name).map(_.toOption))
      billingSchedule <- OptionT(tpBackend.commonPaymentService.billingSchedule(sub.id, numberOfBills = 12))
      suspendableDays = Config.suspendableWeeks * sub.plan.charges.benefits.size
    } yield
      // we can't pattern match on sub as the generic type is lost, so we pattern match on the components.
      // we still then match on the whole thing even though erasure happens, so we have the cast Subscription
      // unfortunately despite that, the compiler doesn't check sub's type matches, so be careful of typos
      sub match {
        case SubMatch(Product.Delivery, charges: PaperCharges, sub: Subscription[PaidSubscriptionPlan[Product.Delivery, PaperCharges]]) =>
          val pendingHolidays = pendingHolidayRefunds(allHolidays)
          val suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, charges.days.toList)
          Ok(views.html.account.suspend(sub, pendingHolidayRefunds(allHolidays), billingSchedule, suspendableDays, suspendedDays, errorCodes))
        case SubMatch(Product.Voucher, _, sub: Subscription[PaidSubscriptionPlan[Product.Voucher, PaperCharges]]) =>
          Ok(views.html.account.voucher(sub, billingSchedule))
        case SubMatch(_: Product.Weekly, _, sub: Subscription[PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, BillingPeriod]]]) =>
          // they can renew
          Ok(views.html.account.renew(sub, billingSchedule))
        case _ =>
          // this wasn't a paper subscription so can't manage
          Ok(views.html.account.details(None))
      }
    ).getOrElse(Ok(views.html.account.details(subscriberId)))
  }

  def logout = accountManagementAction { implicit request =>
    Redirect(ProfileLinks.signOut.href,SEE_OTHER).withSession(request.session - SUBSCRIPTION_SESSION_KEY)
  }

  def processLogin = accountManagementAction.async { implicit request =>
    val loginRequest = AccountManagementLoginForm.mappings.bindFromRequest().value

    subscriptionFromUserDetails(loginRequest).map {
        case Some(sub) => Redirect(routes.AccountManagement.login(None, None)).withSession(
          SUBSCRIPTION_SESSION_KEY -> sub.name.get
        )
        case _ => Redirect(routes.AccountManagement.login(None, None)).flashing(
          "error" -> "Unable to verify your details."
        )
    }
  }

  /**
    * Takes a RefundError error inside a NonEmptyList and prints each error line to the log as a side effect, returning the original NonEmptyList.
    * @param r a NonEmptyList of RefundErrors
    * @return NonEmptyList[RefundError]
    */
  private def getAndLogRefundError(r: ErrNel): ErrNel = {
    r.foreach {
      case e:BadZuoraJson => logger.error(s"Error when adding a new holiday - BadZuoraJson: ${e.got}")
      case e => logger.error(s"Error when adding a new holiday: ${e.code}")
    }
    r
  }

  def processSuspension = accountManagementAction.async { implicit request =>
    lazy val noSub = "Could not find an active subscription"
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      form <- EitherT(Future.successful(SuspendForm.mappings.bindFromRequest().value \/> "Please check your selections and try again"))
      sub <- EitherT(subscriptionFromRequestOld.map(_ \/> noSub).map(_.flatMap(_.swap.leftMap(_ => noSub))))
      newHoliday = PaymentHoliday(sub.name, form.startDate, form.endDate)
      // 14 because + 2 extra months needed in calculation to cover setting a 6-week suspension on the day before your 12th billing day!
      oldBS <- EitherT(tpBackend.commonPaymentService.billingSchedule(sub.id, numberOfBills = 14).map(_ \/> "Error getting billing schedule"))
      result <- EitherT(tpBackend.suspensionService.addHoliday(newHoliday, oldBS)).leftMap(getAndLogRefundError(_).map(_.code).list.mkString(","))
      newBS <- EitherT(tpBackend.commonPaymentService.billingSchedule(sub.id, numberOfBills = 12).map(_ \/> "Error getting billing schedule"))
      newHolidays <- EitherT(tpBackend.suspensionService.getHolidays(sub.name)).leftMap(_ => "Error getting holidays")
      pendingHolidays = pendingHolidayRefunds(newHolidays)
      suspendableDays = Config.suspendableWeeks * sub.plan.charges.days.size
      suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, sub.plan.charges.days.toList)
    } yield {
      tpBackend.exactTargetService.enqueueETHolidaySuspensionEmail(sub, sub.plan.name, newBS, pendingHolidays.size, suspendableDays, suspendedDays).onFailure { case e: Throwable =>
        logger.error(s"Failed to generate data needed to enqueue ${sub.name.get}'s holiday suspension email. Reason: ${e.getMessage}")
      }
      Ok(views.html.account.success(
        newRefund = (result.refund, newHoliday),
        holidayRefunds = pendingHolidays,
        subscription = sub,
        billingSchedule = newBS,
        suspendableDays = suspendableDays,
        suspendedDays = suspendedDays,
        currency = sub.plan.charges.price.currencies.head
      ))
    }).valueOr(errorCode => Redirect(routes.AccountManagement.login(None, Some(errorCode)).url))
  }

  def redirect = NoCacheAction { implicit request =>
    Redirect(routes.AccountManagement.login(None, None).url)
  }
}

