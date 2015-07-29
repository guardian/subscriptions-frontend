package model.exactTarget

import com.gu.membership.zuora.soap.Zuora._
import model.SubscriptionData
import org.joda.time.DateTime

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

    // FIXME bogus payment day while we figure out when to get it properly
    val secondPaymentDate = formatDate(DateTime.now().plusYears(100))

    val personalData = subscriptionData.personalData

    val address = personalData.address

    val paymentData = subscriptionData.paymentData

    SubscriptionDataExtensionRow(
      "SubscriberKey" -> subscription.name,
      "EmailAddress" -> personalData.email,
      "Subscription term" -> billingPeriod,
      "Payment amount" -> ratePlanCharge.price.toString,
      "Default payment method" -> paymentMethod.`type`,
      "First Name" -> personalData.firstName,
      "Last Name" -> personalData.lastName,
      "Address 1" -> address.address1,
      "Address 2" -> address.address2,
      "City" -> address.town,
      "Post Code" -> address.postcode,
      //TODO hardcoded!
      "Country" -> "UK",
      "Account Name" -> paymentData.holder,
      "Sort Code" -> paymentData.sortCode,
      "Account number" -> paymentData.account,
      "Date of first payment" -> formatDate(subscription.termStartDate),
      "Date of second payment" -> secondPaymentDate,
      "Currency" -> account.currency,
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
}

trait DataExtensionRow {
  def fields: Seq[(DataExtensionColumn, String)]
}

case class SubscriptionDataExtensionRow(fields: (String, String)*) extends DataExtensionRow
