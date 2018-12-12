package utils

import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import model.PurchaserIdentifiers
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

object PaymentGatewayError extends LazyLogging {
  // PaymentGatewayError should be logged at WARN level
  def process(e: PaymentGatewayError, purchaserIds: PurchaserIdentifiers, country: String): Result = {

    def handleError(code: String) = {
      logger.warn(s"User $purchaserIds could not subscribe due to payment gateway failed transaction: \n\terror=${e} \n\tuser=$purchaserIds \n\tcountry=$country")
      Forbidden(Json.obj("type" -> "PaymentGatewayError", "code" -> code))
    }

    e.errType match {
      case Fraudulent => handleError("Fraudulent")
      case TransactionNotAllowed => handleError("TransactionNotAllowed")
      case DoNotHonor => handleError("DoNotHonor")
      case InsufficientFunds => handleError("InsufficientFunds")
      case RevocationOfAuthorization => handleError("RevocationOfAuthorization")
      case GenericDecline => handleError("GenericDecline")
      case _ => handleError("UnknownPaymentError")
    }
  }
}
