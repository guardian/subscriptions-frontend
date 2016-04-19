package model.error

import com.gu.memsub.promo.PromoCode
import com.gu.salesforce.ContactId
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.gu.zuora.soap.models.errors.PaymentGatewayError
import services.UserIdData

object CheckoutService {

  sealed trait CheckoutResult

  case class CheckoutSuccess(
      salesforceMember: ContactId,
      userIdData: UserIdData,
      subscribeResult: SubscribeResult,
      validPromoCode: Option[PromoCode]) extends CheckoutResult

  case class CheckoutIdentityFailure(
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutGenericFailure(
      userId: String,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutStripeError(
      userId: String,
      paymentError: Throwable,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutZuoraPaymentGatewayError(
      userId: String,
      paymentError: PaymentGatewayError,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutSalesforceFailure(
      userId: String,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutExactTargetFailure(
      userId: String,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

  case class CheckoutPaymentTypeFailure(
      userId: String,
      msg: String,
      requestData: String,
      errorResponse: Option[String]) extends CheckoutResult with SubsError {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

}
