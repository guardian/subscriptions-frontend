package controllers

import _root_.services.AuthenticationService._
import _root_.services.TouchpointBackend
import actions.CommonActions._
import com.github.nscala_time.time.OrderingImplicits.LocalDateOrdering
import com.gu.memsub.Product
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
      zuoraSubscription <- OptionT(tpBackend.subscriptionService.current[SubscriptionPlan.PaperPlan](salesForceUser).map(_.headOption/*FIXME if they have more than one they can only manage the first*/))
    } yield zuoraSubscription).run
  }

  private def subscriptionFromUserDetails(loginRequestOpt: Option[AccountManagementLoginRequest])(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.PaperPlan]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def detailsMatch(zuoraContact: Contact, loginRequest: AccountManagementLoginRequest): Boolean = {
      def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase

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

    (for {
      sub <- OptionT(subscription)
      allHolidays <- OptionT(tpBackend.suspensionService.getHolidays(sub.name).map(_.toOption))
      billingSchedule <- OptionT(tpBackend.commonPaymentService.billingSchedule(sub.id, numberOfBills = 13))
      chosenPaperDays = sub.plan.charges.chargedDays.toList.sortBy(_.dayOfTheWeekIndex)
      suspendableDays = Config.suspendableWeeks * chosenPaperDays.size
    } yield {
      (sub.plan.product match {
        case Product.Delivery => sub.asDelivery.map { deliverySubscription =>
          val pendingHolidays = pendingHolidayRefunds(allHolidays)
          val suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, deliverySubscription.plan.charges.chargedDays.toList)
          Ok(views.html.account.suspend(deliverySubscription, pendingHolidayRefunds(allHolidays), billingSchedule, chosenPaperDays, suspendableDays, suspendedDays, errorCodes))
        }
        case Product.Voucher => sub.asVoucher.map { voucherSubscription =>
          Ok(views.html.account.voucher(sub, billingSchedule))
        }
        case _: Product.Weekly => sub.asWeekly.map { weeklySubscription =>
          Ok(views.html.account.renew(weeklySubscription, billingSchedule))
        }
      }).getOrElse {
        // the product type didn't have the right charges
        Ok(views.html.account.details(None))
      }
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
      suspendableDays = Config.suspendableWeeks * sub.plan.charges.chargedDays.size
      suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, sub.plan.charges.chargedDays.toList)
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

