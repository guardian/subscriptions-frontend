package model.exactTarget

import com.gu.membership.zuora.soap.models.Queries._
import com.typesafe.scalalogging.LazyLogging
import model.{CreditCardData, DirectDebitData, SubscriptionData}
import org.joda.time.DateTime
import utils.Dates

import scala.math.BigDecimal.decimal

object SubscriptionDataExtensionRow extends LazyLogging{
  def apply(
      subscription: Subscription,
      subscriptionData: SubscriptionData,
      ratePlanCharge: RatePlanCharge,
      paymentMethod: PaymentMethod,
      account: Account
      ): SubscriptionDataExtensionRow = {

    val billingPeriod = ratePlanCharge.billingPeriod.getOrElse(
      throw new ExactTargetException(s"Error while processing $subscription: Could not create a SubscriptionDataExtensionRow without a billing period")
    )

    val personalData = subscriptionData.personalData

    val address = personalData.address

    val paymentFields = subscriptionData.paymentData match {
      case DirectDebitData(accountNumber, sortCode, holder) =>
        val mandateParam = paymentMethod.mandateId match {
          case Some(id) => Seq("MandateID" -> id)
          case None =>
            logger.error(s"Expected the payment method for subscription ${subscription.name} to have a mandate id (Direct Debit payment).")
            Seq()
        }

        Seq(
          "Account number" -> formatAccountNumber(accountNumber),
          "Sort Code" -> formatSortCode(sortCode),
          "Account Name" -> holder,
          "Default payment method" -> "Direct Debit"
        ) ++ mandateParam

      case CreditCardData(_) => Seq("Default payment method" -> "Credit/Debit Card")
    }

    SubscriptionDataExtensionRow(
      personalData.email,
      Seq(
        "ZuoraSubscriberId" -> subscription.name,
        "SubscriberKey" -> personalData.email,
        "EmailAddress" -> personalData.email,
        "Subscription term" -> formatSubscriptionTerm(billingPeriod),
        "Payment amount" -> formatPrice(ratePlanCharge.price),
        "First Name" -> personalData.firstName,
        "Last Name" -> personalData.lastName,
        "Address 1" -> address.lineOne,
        "Address 2" -> address.lineTwo,
        "City" -> address.town,
        "Post Code" -> address.postCode,
        //TODO hardcoded!
        "Country" -> "UK",
        "Date of first payment" -> formatDate(subscription.contractAcceptanceDate),
        "Currency" -> formatCurrency(account.currency),
        //TODO to remove, hardcoded in the template
        "Trial period" -> "14",
        "Email" -> personalData.email
      ) ++ paymentFields
    )
  }

  private def formatDate(dateTime: DateTime) = {
    val day = dateTime.dayOfMonth.get

    val dayWithSuffix = Dates.getOrdinalDay(day)

    val month = dateTime.monthOfYear.getAsText

    val year = dateTime.year.getAsString

    s"$dayWithSuffix $month $year"
  }

  private def formatPrice(price: Float): String = {
    decimal(price).bigDecimal.stripTrailingZeros.toPlainString
  }

  private def formatSubscriptionTerm(term: String): String = {
    term match {
      case "Annual" => "year"
      case otherTerm => otherTerm.toLowerCase
    }
  }

  private def formatPaymentMethod(method: String): String = {
    method match {
      case "BankTransfer" => "Direct Debit"
      case otherMethod => otherMethod
    }
  }

  private def formatAccountNumber(AccountNumber: String): String = {
    val lastFour = AccountNumber takeRight 4
    s"****$lastFour"
  }

  private def formatSortCode(sortCode: String): String = {
    sortCode.grouped(2).mkString("-")
  }

  private def formatCurrency(currency: String): String = {
    currency match {
      case "GBP" => "£"
      case other => other
    }
  }
}

trait DataExtensionRow {
  def fields: Seq[(String, String)]
}

case class SubscriptionDataExtensionRow(email: String, fields: Seq[(String, String)]) extends DataExtensionRow
