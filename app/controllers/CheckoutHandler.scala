package controllers

import java.util.UUID

import actions.CommonActions
import cats.data.EitherT
import cats.instances.future._
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.memsub.SupplierCode
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.monitoring.SafeLogger._
import com.typesafe.scalalogging.LazyLogging
import forms.SubscriptionsForm
import model._
import model.error.CheckoutService._
import model.error.SubsError
import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.mvc._
import scalaz.NonEmptyList
import services.AuthenticationService.authenticatedUserFor
import services._
import utils.RequestCountry._
import utils.{PaymentGatewayError, TestUsers}
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.support.{BillingPeriod => _}

import scala.concurrent.{ExecutionContext, Future}

class CheckoutHandler(fBackendFactory: TouchpointBackends, commonActions: CommonActions, implicit val executionContext: ExecutionContext, override protected val controllerComponents: ControllerComponents) extends BaseController with LazyLogging {
  import SessionKeys.{Currency => _, UserId => _, _}
  import commonActions._

  def checkoutService(implicit res: TouchpointBackends.Resolution): CheckoutService =
    res.backend.checkoutService

  def handleCheckout: Action[AnyContent] = NoCacheAction.async { implicit request =>

    val tempData = SubscriptionsForm.subsForm.bindFromRequest().value
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(NameEnteredInForm, tempData)
    implicit val tpBackend: TouchpointBackend = resolution.backend
    val idUserOpt = authenticatedUserFor(request)

    val sessionTrackingCode = request.session.get(PromotionTrackingCode)
    val sessionSupplierCode = request.session.get(SupplierTrackingCode)

    tpBackend.subsForm.flatMap { subsForm =>

      val preliminarySubscribeRequest = {
        subsForm.bindFromRequest.valueOr {
          e =>
            throw new Exception(scrub"Backend validation failed: identityId=${idUserOpt.map(_.user.id).mkString};" +
              s" JavaScriptEnabled=${request.headers.toMap.contains("X-Requested-With")};" +
              s" ${e.map(err => s"${err.key} ${err.message}").mkString(", ")}")
        }
      }
      val subscribeRequest = preliminarySubscribeRequest.copy(
        genericData = preliminarySubscribeRequest.genericData.copy(
          promoCode = preliminarySubscribeRequest.genericData.promoCode orElse sessionTrackingCode.map(PromoCode)
        )
      )

      val requestData = SubscriptionRequestData(
        ipCountry = request.getFastlyCountry,
        supplierCode = sessionSupplierCode.map(SupplierCode)
      )

      val checkoutResult = checkoutService.processSubscription(subscribeRequest, idUserOpt, requestData)

      def failure(seqErr: NonEmptyList[SubsError]) = {
        Future.successful(
          seqErr.head match {
            case e: CheckoutIdentityFailure =>
              logger.error(SubsError.header(e))
              logger.warn(SubsError.toStringPretty(e))

              Forbidden(Json.obj("type" -> "CheckoutIdentityFailure",
                "message" -> "User could not subscribe because Identity Service could not register the user"))

            case e: CheckoutStripeError =>
              logger.warn(SubsError.toStringPretty(e))

              Forbidden(Json.obj(
                "type" -> "CheckoutStripeError",
                "message" -> e.paymentError.getMessage))

            case e: CheckoutZuoraPaymentGatewayError =>
              logger.warn(SubsError.toStringPretty(e))
              PaymentGatewayError.process(e.paymentError, e.purchaserIds, subscribeRequest.genericData.personalData.address.countryName)

            case e: CheckoutPaymentTypeFailure =>
              logger.error(SubsError.header(e))
              logger.warn(SubsError.toStringPretty(e))

              Forbidden(Json.obj("type" -> "CheckoutPaymentTypeFailure",
                "message" -> e.msg))

            case e: CheckoutSalesforceFailure =>
              logger.error(SubsError.header(e))
              logger.warn(SubsError.toStringPretty(e))

              Forbidden(Json.obj("type" -> "CheckoutSalesforceFailure",
                "message" -> e.msg))

            case e: CheckoutGenericFailure =>
              logger.error(SubsError.header(e))
              logger.warn(SubsError.toStringPretty(e))

              Forbidden(Json.obj("type" -> "CheckoutGenericFailure",
                "message" -> "User could not subscribe"))
          }
        )
      }

      def success(r: CheckoutSuccess) = {

        r.nonFatalErrors.map(e => SubsError.header(e) -> SubsError.toStringPretty(e)).foreach {
          case (h, m) =>
            logger.warn(h)
            logger.error(m)
        }
        val productData = Seq(
          SubsName -> r.subscribeResult.subscriptionName,
          RatePlanId -> subscribeRequest.productRatePlanId.get,
          SessionKeys.Currency -> subscribeRequest.genericData.currency.toString,
          BillingCountryName -> subscribeRequest.genericData.personalData.address.countryName
        )

        val userSessionFields = r.userIdData match {
          case Some(GuestUser(UserId(userId), IdentityToken(token))) =>
            Seq(SessionKeys.UserId -> userId, IdentityGuestPasswordSettingToken -> token)
          case _ => Seq()
        }

        val appliedCode = r.validPromotion.flatMap(vp => vp.promotion.asTracking.fold[Option[PromoCode]](Some(vp.code))(_ => None))
        val appliedCodeSession = appliedCode.map(promoCode => Seq(AppliedPromoCode -> promoCode.get)).getOrElse(Seq.empty)
        val subscriptionDetails = Some(StartDate -> subscribeRequest.productData.fold(_.startDate, _ => LocalDate.now).toString("d MMMM YYYY"))
        val marketingOptIn = Seq(MarketingOptIn -> subscribeRequest.genericData.personalData.receiveGnmMarketing.toString)
        // Don't remove the SupplierTrackingCode from the session
        val session = (productData ++ userSessionFields ++ marketingOptIn ++ appliedCodeSession ++ subscriptionDetails).foldLeft(request.session - AppliedPromoCode - PromotionTrackingCode) {
          _ + _
        }

        submitAcquisitionEvent(request, subscribeRequest).value.map { _ =>
          logger.info(s"User successfully became subscriber:\n\tUser: SF=${r.salesforceMember.salesforceContactId}\n\tPlan: ${subscribeRequest.productData.fold(_.plan, _.plan).name}\n\tSubscription: ${r.subscribeResult.subscriptionName}")
          Ok(Json.obj("redirect" -> routes.Checkout.thankYou().url)).withSession(session)
        }
      }

      def submitAcquisitionEvent(request: Request[AnyContent], subscribeRequest: SubscribeRequest): EitherT[Future, Unit, AcquisitionSubmission] = {
        val testUser = TestUsers.isTestUser(PreSigninTestCookie, request.cookies)(request)
        if(testUser.isEmpty) {
          val acquisitionData = request.session.get("acquisitionData")
          if (acquisitionData.isEmpty) {
            logger.warn(s"No acquisitionData in session")
          }

          val promotion = subscribeRequest.genericData.promoCode.map(_.get).flatMap(code => tpBackend.promoService.findPromotion(NormalisedPromoCode.safeFromString(code)))
          val clientBrowserInfo = ClientBrowserInfo(
            request.cookies.get("_ga").map(_.value).getOrElse(UUID.randomUUID().toString), //Get GA client id from cookie
            request.headers.get("user-agent"),
            request.remoteAddress
          )

          //This service is mocked in DEV unless analytics.onInDev=true in application.config
          AcquisitionService(tpBackend.environmentName)
            .submit(SubscriptionAcquisitionComponents(subscribeRequest, promotion, acquisitionData, clientBrowserInfo))
            .leftMap(
              err => logger.warn(s"Error submitting acquisition data. $err")
            )
        } else {
          val either: Future[Either[Unit, AcquisitionSubmission]] = Future.successful(Left(Unit))
          EitherT(either) // Don't submit acquisitions for test users
        }
      }

      checkoutResult.flatMap(_.fold(failure, success))

    }

  }
}
