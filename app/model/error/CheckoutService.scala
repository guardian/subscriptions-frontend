package model.error

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.promo.PromoCode
import com.gu.salesforce.ContactId
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors.PaymentGatewayError
import model.PurchaserIdentifiers
import services.UserIdData

object CheckoutService {

  sealed trait CheckoutResult

  case class CheckoutSuccess(
      salesforceMember: ContactId,
      userIdData: Option[UserIdData],
      subscribeResult: SubscribeResult,
      validPromoCode: Option[PromoCode],
      nonFatalErrors: Seq[SubsError]) extends CheckoutResult

  case class CheckoutIdentityFailure(
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutGenericFailure(
      purchaserIds: PurchaserIdentifiers,
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutStripeError(
      purchaserIds: PurchaserIdentifiers,
      paymentError: Throwable,
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutZuoraPaymentGatewayError(
      purchaserIds: PurchaserIdentifiers,
      paymentError: PaymentGatewayError,
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutSalesforceFailure(
      identityUser: Option[IdMinimalUser],
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutExactTargetFailure(
      purchaserIds: PurchaserIdentifiers,
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutPaymentTypeFailure(
      purchaserIds: PurchaserIdentifiers,
      msg: String,
      requestData: Option[String] = None,
      errorResponse: Option[String] = None) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

}


