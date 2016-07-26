package controllers

import actions.CommonActions._
import com.gu.memsub.Subscription
import com.gu.zuora.soap.models.Queries.Contact
import com.typesafe.scalalogging.LazyLogging
import forms.{AccountManagementLoginForm, AccountManagementLoginRequest, SuspendForm, Suspension}
import play.api.mvc.{AnyContent, Controller, Request}
import services.AuthenticationService._
import services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{Monad, OptionT}
import scalaz.std.scalaFuture._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.option._

object AccountManagement extends Controller with LazyLogging {

  val SUBSCRIPTION_SESSION_KEY = "subscriptionId"

  private def subscriptionFromRequest(implicit request: Request[AnyContent]): Future[Option[Subscription]] = {
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    (for {
      subscriptionId <- OptionT(Future.successful(request.session.data.get(SUBSCRIPTION_SESSION_KEY)))
      zuoraSubscription <- OptionT(tpBackend.subscriptionServicePaper.get(Subscription.Name(subscriptionId)))
    } yield zuoraSubscription).orElse(for {
      identityUser <- OptionT(Future.successful(authenticatedUserFor(request)))
      salesForceUser <- OptionT(tpBackend.salesforceService.repo.get(identityUser.user.id))
      zuoraSubscription <- OptionT(tpBackend.subscriptionServicePaper.getPaid(_.findPhysical)(salesForceUser))
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

  def login(subscriberId: Option[String] = None) = NoCacheAction.async { implicit request =>

    val subscription = subscriptionFromRequest(request)

    subscription.map {
      case Some(sub) => {
        val signedSubscriptionId = Suspension.signedSubscriptionId(sub.name.get)
        Ok(views.html.account.suspend(sub.name.get, signedSubscriptionId))
      }
      case _ => {
        Ok(views.html.account.details(subscriberId))
      }
    }

  }

  def processLogin = NoCacheAction.async { implicit request =>
    val loginRequest = AccountManagementLoginForm.mappings.bindFromRequest().value

    subscriptionFromUserDetails(loginRequest).map {
        case Some(sub) => Redirect(routes.AccountManagement.login(None)).withSession(
          SUBSCRIPTION_SESSION_KEY -> sub.name.get
        )
        case _ => Redirect(routes.AccountManagement.login(None)).flashing(
          "error" -> "Unable to verify your details."
        )
    }
  }

  def processSuspension = NoCacheAction.async { implicit request =>
    val suspension = SuspendForm.mappings.bindFromRequest().value.map(s => Seq(
        s"Got ${s.verifiedSubscriptionId.getOrElse("unverified subscription ID")}",
        s"Asked to suspend ${s.subscriptionId} for ${s.asDays()} days, starting ${s.startDate}",
        s"Suspension = ${s.toString}", s"s.verifiedSubscriptionId = ${s.verifiedSubscriptionId}"
      ).mkString("\n")
    ).getOrElse("No data provided")
    Future.successful(Ok(suspension).withNewSession)
  }

}

