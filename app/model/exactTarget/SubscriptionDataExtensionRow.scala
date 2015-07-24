package model.exactTarget

import com.gu.membership.zuora.soap.Zuora._
import model.SubscriptionData

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
      `SubscriberKey` -> subscription.name,
      `EmailAddress` -> personalData.email,
      `Subscription term` -> billingPeriod,
      `Payment amount` -> ratePlanCharge.price.toString,
      `Default payment method` -> paymentMethod.`type`,
      `First Name` -> personalData.firstName,
      `Last Name` -> personalData.lastName,
      `Address 1` -> address.address1,
      `Address 2` -> address.address2,
      `City` -> address.town,
      `Post Code` -> address.postcode,
      //TODO hardcoded!
      `Country` -> "UK",
      `Account Name` -> paymentData.holder,
      `Sort Code` -> paymentData.sortCode,
      `Account number` -> paymentData.account,
      `Date of first payment` -> subscription.termStartDate.toString,
      `Date of second payment` -> ratePlanCharge.chargedThroughDate.toString,
      `Currency` -> account.currency,
      //TODO to remove, hardcoded in the template
      `Trial period` -> "14",
      `MandateID` -> paymentMethod.mandateId,
      `Email` -> personalData.email
    )
  }
}

trait DataExtensionRow {
  def fields: Seq[(DataExtensionColumn, String)]
}

case class SubscriptionDataExtensionRow(fields: (DataExtensionColumn, String)*) extends DataExtensionRow
