package controllers

import actions.CommonActions._
import com.github.nscala_time.time.OrderingImplicits.LocalDateOrdering
import com.gu.memsub.{Delivery, PaidPS, Subscription}
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.subscriptions.suspendresume.SuspensionService.{BadZuoraJson, ErrNel, HolidayRefund, PaymentHoliday}
import com.gu.subscriptions.{PhysicalProducts, ProductPlan}
import com.gu.zuora.soap.models.Queries.Contact
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import forms.{AccountManagementLoginForm, AccountManagementLoginRequest, SuspendForm}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Controller, Request}
import services.AuthenticationService._
import services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.{EitherT, OptionT}

object AccountManagement extends Controller with LazyLogging {

  val SUBSCRIPTION_SESSION_KEY = "subscriptionId"

  private def subscriptionFromRequest(implicit request: Request[AnyContent]): Future[Option[Subscription with PaidPS[ProductPlan[PhysicalProducts]]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      subscriptionId <- OptionT(Future.successful(request.session.data.get(SUBSCRIPTION_SESSION_KEY)))
      zuoraSubscription <- OptionT(tpBackend.subscriptionServicePaper.paidWith(Subscription.Name(subscriptionId))(_.findPhysical))
    } yield zuoraSubscription).orElse(for {
      identityUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      salesForceUser <- OptionT(tpBackend.salesforceService.repo.get(identityUser.user.id))
      zuoraSubscription <- OptionT(tpBackend.subscriptionServicePaper.paidWith(salesForceUser)(_.findPhysical))
    } yield zuoraSubscription).run
  }

  private def subscriptionFromUserDetails(loginRequestOpt: Option[AccountManagementLoginRequest])(implicit request: Request[AnyContent]): Future[Option[Subscription]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def detailsMatch(zuoraContact: Contact, loginRequest: AccountManagementLoginRequest): Boolean = {
      def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase

      format(zuoraContact.lastName) == format(loginRequest.lastname) &&
        zuoraContact.postalCode.map(format).contains(format(loginRequest.postcode))
    }

    (for {
      loginRequest <- OptionT(Future.successful(loginRequestOpt))
      zuoraSubscription <- OptionT(tpBackend.subscriptionServicePaper.get(Subscription.Name(loginRequest.subscriptionId)))
      zuoraAccount <- OptionT(tpBackend.zuoraService.getAccount(zuoraSubscription.accountId).map(a => Option(a)))
      zuoraContact <- OptionT(tpBackend.zuoraService.getContact(zuoraAccount.billToId).map(c => Option(c)))
      _ <- OptionT(Future.successful(detailsMatch(zuoraContact, loginRequest).unlessM[Option, Nothing](None)))
    } yield zuoraSubscription).run

  }

  private def pendingHolidayRefunds(allHolidays: Seq[HolidayRefund]) = allHolidays.filterNot(_._2.finish.isBefore(LocalDate.now)).sortBy(_._2.start)

  def login(subscriberId: Option[String] = None, errorCode: Option[String] = None) = GoogleAuthenticatedStaffAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val subscription = subscriptionFromRequest
    val errorCodes = errorCode.toSeq.flatMap(_.split(',').map(_.trim)).filterNot(_.isEmpty).toSet

    (for {
      sub <- OptionT(subscription)
      allHolidays <- OptionT(tpBackend.suspensionService.getHolidays(sub.name).map(_.toOption))
      billingSchedule <- OptionT(tpBackend.commonPaymentService.billingSchedule(sub, 12))
      suspendableDays = Config.suspendableWeeks * sub.plan.products.without(Delivery).size
    } yield {
      sub.plan.products.seq.contains(Delivery).fold({
        val pendingHolidays = pendingHolidayRefunds(allHolidays)
        val suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, sub.plan.products.physicalProducts.list)
        Ok(views.html.account.suspend(sub, pendingHolidayRefunds(allHolidays), billingSchedule, suspendableDays, suspendedDays, errorCodes))
      },{
        Ok(views.html.account.voucher(sub, billingSchedule))
      })
    }).getOrElse(Ok(views.html.account.details(subscriberId)))
  }

  def processLogin = GoogleAuthenticatedStaffAction.async { implicit request =>
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

  def processSuspension = GoogleAuthenticatedStaffAction.async { implicit request =>
    lazy val noSub = "Could not find an active subscription"
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      form <- EitherT(Future.successful(SuspendForm.mappings.bindFromRequest().value \/> "Please check your selections and try again"))
      sub <- EitherT(subscriptionFromRequest.map(_ \/> noSub))
      newHoliday = PaymentHoliday(sub.name, form.startDate, form.endDate)
      // 14 because + 2 extra months needed in calculation to cover setting a 6-week suspension on the day before your 12th billing day!
      oldBS <- EitherT(tpBackend.commonPaymentService.billingSchedule(sub, 14).map(_ \/> "Error getting billing schedule"))
      result <- EitherT(tpBackend.suspensionService.addHoliday(newHoliday, oldBS)).leftMap(getAndLogRefundError(_).map(_.code).list.mkString(","))
      newBS <- EitherT(tpBackend.commonPaymentService.billingSchedule(sub, 12).map(_ \/> "Error getting billing schedule"))
      newHolidays <- EitherT(tpBackend.suspensionService.getHolidays(sub.name)).leftMap(_ => "Error getting holidays")
      pendingHolidays = pendingHolidayRefunds(newHolidays)
      suspendableDays = Config.suspendableWeeks * sub.plan.products.without(Delivery).size
      suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, sub.plan.products.physicalProducts.list)
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
        currency = sub.currency
      )).withNewSession
    }).valueOr(errorCode => Redirect(routes.AccountManagement.login(None, Some(errorCode)).url))
  }

  def redirect = NoCacheAction { implicit request =>
    Redirect(routes.AccountManagement.login(None, None).url)
  }
}

