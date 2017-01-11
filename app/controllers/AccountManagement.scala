package controllers

import _root_.services.AuthenticationService._
import _root_.services.TouchpointBackend
import _root_.services.TouchpointBackend.Resolution
import actions.CommonActions._
import com.github.nscala_time.time.OrderingImplicits.LocalDateOrdering
import com.gu.memsub.subsv2.SubscriptionPlan.{Delivery, WeeklyPlan}
import com.gu.memsub.Subscription.{Name, ProductRatePlanId}
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.memsub.services.GetSalesforceContactForSub
import com.gu.memsub.subsv2._
import com.gu.memsub.{BillingSchedule, Product}
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.subscriptions.suspendresume.SuspensionService.{BadZuoraJson, ErrNel, HolidayRefund, PaymentHoliday}
import com.gu.zuora.soap.models.Queries.Contact
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, ProfileLinks}
import forms._
import model.{Renewal, RenewalReads}
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.mvc.{AnyContent, Controller, Request, Result}
import utils.TestUsers.PreSigninTestCookie

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.{-\/, EitherT, OptionT, \/, \/-}
import model.SubscriptionOps._
import views.html.account.thankYouRenew
import views.support.Pricing._

// this handles putting subscriptions in and out of the session
object SessionSubscription {

  val SUBSCRIPTION_SESSION_KEY = "subscriptionId"

  def set(result: Result, sub: Subscription[SubscriptionPlan.PaperPlan]) =
    result.withSession(
      SUBSCRIPTION_SESSION_KEY -> sub.name.get
    )

  def clear(result: Result)(implicit request: Request[AnyContent]): Result =
    result.withSession(request.session - SUBSCRIPTION_SESSION_KEY)

  @deprecated("use the other one and pattern match on the plan")
  def subscriptionFromRequestOld(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.Delivery] \/ Subscription[SubscriptionPlan.Voucher]]] = {
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

  def subscriptionFromRequest(implicit request: Request[_]): Future[Option[Subscription[SubscriptionPlan.PaperPlan]]] = {
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

}

object ManageDelivery extends LazyLogging{

  import play.api.mvc.Results._

  def apply(errorCodes: Set[String], allHolidays: Seq[(Float, PaymentHoliday)], billingSchedule: BillingSchedule, deliverySubscription: Subscription[Delivery])(implicit request: Request[AnyContent], touchpoint: TouchpointBackend.Resolution) = {
    val pendingHolidays = pendingHolidayRefunds(allHolidays)
    val suspendedDays = SuspensionService.holidayToSuspendedDays(pendingHolidays, deliverySubscription.plan.charges.chargedDays.toList)
    val chosenPaperDays = deliverySubscription.plan.charges.chargedDays.toList.sortBy(_.dayOfTheWeekIndex)
    val suspendableDays = Config.suspendableWeeks * chosenPaperDays.size
    Ok(views.html.account.suspend(deliverySubscription, pendingHolidayRefunds(allHolidays), billingSchedule, chosenPaperDays, suspendableDays, suspendedDays, errorCodes))
  }

  private def pendingHolidayRefunds(allHolidays: Seq[HolidayRefund]) = allHolidays.filterNot(_._2.finish.isBefore(LocalDate.now)).sortBy(_._2.start)

  def suspend(tpBackend: TouchpointBackend)(implicit request: Request[AnyContent], touchpoint: TouchpointBackend.Resolution): Future[Result] = {
    val noSub = "Could not find an active subscription"
    (for {
      form <- EitherT(Future.successful(SuspendForm.mappings.bindFromRequest().value \/> "Please check your selections and try again"))
      sub <- EitherT(SessionSubscription.subscriptionFromRequestOld.map(_ \/> noSub).map(_.flatMap(_.swap.leftMap(_ => noSub))))
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
        currency = sub.currency
      ))
    }).valueOr(errorCode => Redirect(routes.AccountManagement.manage(None, Some(errorCode),None).url))
  }

  /**
    * Takes a RefundError error inside a NonEmptyList and prints each error line to the log as a side effect, returning the original NonEmptyList.
    *
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

}

object ManageWeekly extends LazyLogging {

  import play.api.mvc.Results._

  // this sequencing concatenates errors if any, otherwise aggregates rights
  def sequence[A](list: List[Either[String, A]]): Either[String, List[A]] = {
    val errors = list collect {
      case Left(x) => x
    }
    if (errors.nonEmpty)
      Left(errors.mkString(", "))
    else
      Right(list collect {
        case Right(x) => x
      })
  }

  case class WeeklyPlanInfo(id: ProductRatePlanId, price: String)

  object WeeklyPlanInfo {

    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit def writer: Writes[WeeklyPlanInfo] =
      (
        (JsPath \ "id").write[String].contramap[ProductRatePlanId](_.get) and
          (JsPath \ "price").write[String]
        )(unlift(WeeklyPlanInfo.unapply))

  }

  def apply(billingSchedule: BillingSchedule, weeklySubscription: Subscription[WeeklyPlan], promoCode: Option[PromoCode])(implicit request: Request[AnyContent], resolution: TouchpointBackend.Resolution): Future[Result] = {
    implicit val tpBackend = resolution.backend
    implicit val flash = request.flash
    if (weeklySubscription.readerType == ReaderType.Direct) {
      // go to SF to get the contact mailing information etc
      GetSalesforceContactForSub.zuoraAccountFromSub(weeklySubscription)(tpBackend.zuoraService, tpBackend.salesforceService.repo, global).flatMap { account =>
        val futureSfContact = GetSalesforceContactForSub.sfContactForZuoraAccount(account)(tpBackend.zuoraService, tpBackend.salesforceService.repo, global)
        val futureZuoraBillToContact = tpBackend.zuoraService.getContact(account.billToId)
        futureSfContact.flatMap { contact =>
          futureZuoraBillToContact.map { zuoraContact =>
            zuoraContact.country.map { billToCountry =>
              val catalog = tpBackend.catalogService.unsafeCatalog
              val weeklyPlans = weeklySubscription.planToManage.product match {
                case Product.WeeklyZoneA => catalog.weeklyZoneA.toList
                case Product.WeeklyZoneB => catalog.weeklyZoneB.toList
                case Product.WeeklyZoneC => catalog.weeklyZoneC.toList
              }
              val currency = account.currency.toRight(s"could not deserialise currency for account ${account.id}")
              val weeklyPlanInfo = currency.right.flatMap { existingCurrency =>
                sequence(weeklyPlans.map { plan =>
                  val price = plan.charges.price.getPrice(existingCurrency).toRight(s"could not find price in $existingCurrency for plan ${plan.id} ${plan.name}").right
                  price.map(price => WeeklyPlanInfo(plan.id, plan.charges.prettyPricing(price.currency)))
                }).right.map(r => (existingCurrency, r))
              }
              weeklyPlanInfo match {
                case Left(error) =>
                  logger.info(s"couldn't get new rate/currency for renewal: $error")
                  Ok(views.html.account.details(None, promoCode))
                case Right((existingCurrency, weeklyPlanInfoList)) =>
                  Ok(weeklySubscription.asRenewable.map { renewableSub =>
                    views.html.account.weeklyRenew(renewableSub, billingSchedule, contact, billToCountry, weeklyPlanInfoList, existingCurrency, promoCode)
                  } getOrElse {
                    views.html.account.weeklyDetails(weeklySubscription, billingSchedule, contact)
                  })
              }
            }.getOrElse {
              logger.info(s"no valid bill to country for ${weeklySubscription.id}")
              Ok(views.html.account.details(None,promoCode))
            }
          }
        }
      }
    } else {
      logger.info(s"don't support gifts, can't manage ${weeklySubscription.id}")
      Future.successful(Ok(views.html.account.details(None,promoCode))) // don't support gifts (yet) as they have related contacts in salesforce of unknown structure
    }
  }

}

object AccountManagement extends Controller with LazyLogging with CatalogProvider {

  val accountManagementAction = NoCacheAction

  def subscriptionFromUserDetails(loginRequestOpt: Option[AccountManagementLoginRequest])(implicit request: Request[AnyContent]): Future[Option[Subscription[SubscriptionPlan.PaperPlan]]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def detailsMatch(zuoraContact: Contact, loginRequest: AccountManagementLoginRequest): Boolean = {
      def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase
      format(zuoraContact.lastName) == format(loginRequest.lastname)
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

  def manage(subscriberId: Option[String] = None, errorCode: Option[String] = None, promoCode: Option[PromoCode]= None) = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val eventualMaybeSubscription = SessionSubscription.subscriptionFromRequest
    val errorCodes = errorCode.toSeq.flatMap(_.split(',').map(_.trim)).filterNot(_.isEmpty).toSet

    val futureMaybeFutureManagePage = for {
      subscription <- OptionT(eventualMaybeSubscription)
      allHolidays <- OptionT(tpBackend.suspensionService.getHolidays(subscription.name).map(_.toOption))
      billingSchedule <- OptionT(tpBackend.commonPaymentService.billingSchedule(subscription.id, numberOfBills = 13))
    } yield {
      val maybeFutureManagePage = subscription.planToManage.product match {
        case Product.Delivery => subscription.asDelivery.map { deliverySubscription =>
          Future.successful(ManageDelivery(errorCodes, allHolidays, billingSchedule, deliverySubscription))
        }
        case Product.Voucher => subscription.asVoucher.map { voucherSubscription =>
          Future.successful(Ok(views.html.account.voucher(voucherSubscription, billingSchedule)))
        }
        case _: Product.Weekly => subscription.asWeekly.map { weeklySubscription =>
          ManageWeekly(billingSchedule, weeklySubscription, promoCode)
        }
      }
      maybeFutureManagePage.getOrElse {
        // the product type didn't have the right charges
        Future.successful(Ok(views.html.account.details(None, promoCode)))
      }
    }

    futureMaybeFutureManagePage.getOrElse {
      // not a valid AS number or some unnamed problem getting the details
      Future.successful(Ok(views.html.account.details(subscriberId, promoCode)))
    }.flatMap(identity)
  }

  def logout = accountManagementAction { implicit request =>
    SessionSubscription.clear(Redirect(ProfileLinks.signOut.href, SEE_OTHER))
  }

  def processLogin = accountManagementAction.async { implicit request =>
    val loginRequest = AccountManagementLoginForm.mappings.bindFromRequest().value
    val promoCode = loginRequest.flatMap(_.promoCode).map(NormalisedPromoCode.safeFromString)
    subscriptionFromUserDetails(loginRequest).map {
        case Some(sub) => SessionSubscription.set(Redirect(routes.AccountManagement.manage(None,None,promoCode)), sub)
        case _ => Redirect(routes.AccountManagement.manage(None, None,None)).flashing(
          "error" -> "Unable to verify your details."
        )
    }
  }

  def processSuspension = accountManagementAction.async { implicit request =>
    implicit val resolution: Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    ManageDelivery.suspend(tpBackend)
  }

  def redirect = NoCacheAction { implicit request =>
    Redirect(routes.AccountManagement.manage(None, None, None).url)
  }

  def processRenewal = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    def jsonError(message: String) = Json.toJson(Json.obj("errorMessage" -> message))

    def description(sub: Subscription[SubscriptionPlan.WeeklyPlan], renewal: Renewal) = renewal.plan.charges.prettyPricing(sub.currency)

    SessionSubscription.subscriptionFromRequest flatMap { maybeSub =>
      val response = for {
        sub <- maybeSub.toRightDisjunction("no subscription in request")
        weeklySub <- sub.asWeekly.toRightDisjunction("subscription is not weekly")
        renewableSub <- weeklySub.asRenewable.toRightDisjunction("subscription is not renewable")
        renew <- parseRenewalRequest(request, tpBackend.catalogService.unsafeCatalog)
      } yield {
        logger.info(s"renewing ${renew.plan.name} for ${renewableSub.id} with promo code: ${renew.promoCode}")
        tpBackend.checkoutService.renewSubscription(renewableSub, renew, description(renewableSub, renew)).map(res => Ok(Json.obj("redirect" -> routes.AccountManagement.renewThankYou.url)))
        .recover{
          case e: Throwable =>
            val errorMessage = "Unexpected error while renewing subscription"
            logger.error(errorMessage, e)
            InternalServerError(jsonError(errorMessage))
        }
      }
      response.valueOr(error => Future(BadRequest(jsonError(error))))
    }
  }

  def parseRenewalRequest(request: Request[AnyContent], catalog: Catalog): \/[String, Renewal] = {
    implicit val renewalReads = new RenewalReads(catalog).renewalReads
    request.body.asJson.map(_.validate[Renewal]) match {
      case Some(JsSuccess(renewal, _)) => \/-(renewal)
      case Some(JsError(err)) => -\/(err.mkString(","))
      case None => -\/("invalid json")
    }
  }

  def renewThankYou() = accountManagementAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    import model.SubscriptionOps._

    val res = for {
    subscription <- OptionT( SessionSubscription.subscriptionFromRequest)
    billingSchedule <- OptionT(tpBackend.commonPaymentService.billingSchedule(subscription.id, numberOfBills = 13))
    }yield {
      Ok(thankYouRenew(subscription.nextPlan,billingSchedule, resolution))
    }
    res.run.map(_.getOrElse(Redirect(routes.Homepage.index()).withNewSession))
  }
}
