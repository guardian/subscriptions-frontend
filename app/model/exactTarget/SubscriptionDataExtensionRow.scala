package model.exactTarget

import com.gu.membership.zuora.soap.Zuora._
import model.SubscriptionData
import org.joda.time.DateTime
import scala.math.BigDecimal.decimal

object SubscriptionDataExtensionRow {
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

    val paymentData = subscriptionData.paymentData

    SubscriptionDataExtensionRow(
      subscription.name,
      "SubscriberKey" -> personalData.email,
      "EmailAddress" -> personalData.email,
      "Subscription term" -> formatSubscriptionTerm(billingPeriod),
      "Payment amount" -> formatPrice(ratePlanCharge.price),
      "Default payment method" -> formatPaymentMethod(paymentMethod.`type`),
      "First Name" -> personalData.firstName,
      "Last Name" -> personalData.lastName,
      "Address 1" -> address.address1,
      "Address 2" -> address.address2,
      "City" -> address.town,
      "Post Code" -> address.postcode,
      //TODO hardcoded!
      "Country" -> "UK",
      "Account Name" -> paymentData.holder,
      "Sort Code" -> formatSortCode(paymentData.sortCode),
      "Account number" -> formatAccountNumber(paymentData.account),
      "Date of first payment" -> formatDate(subscription.contractAcceptanceDate),
      "Currency" -> formatCurrency(account.currency),
      //TODO to remove, hardcoded in the template
      "Trial period" -> "14",
      "MandateID" -> paymentMethod.mandateId,
      "Email" -> personalData.email
    )
  }

  private def formatDate(dateTime: DateTime) = {
    val day = dateTime.dayOfMonth.getAsString
    val daySuffix = day.last match {
      case '1' => "st"
      case '2' => "nd"
      case '3' => "rd"
      case _   => "th"
    }
    val month = dateTime.monthOfYear.getAsText

    val year = dateTime.year.getAsString

    s"$day$daySuffix $month $year"
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

case class SubscriptionDataExtensionRow(subscriptionId: String, fields: (String, String)*) extends DataExtensionRow
