package model.exactTarget
import com.gu.memsub.Subscription
import com.gu.memsub.Subscription._
import com.typesafe.scalalogging.LazyLogging
import com.gu.memsub.PaymentMethod
import org.joda.time.{Days, LocalDate}
import model.PersonalData
import utils.Dates

import scala.math.BigDecimal.decimal

object SubscriptionDataExtensionRow extends LazyLogging {

  def apply(
             personalData: PersonalData,
             subscription: Subscription with Paid,
             paymentMethod: PaymentMethod,
             gracePeriod: Days,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None,
             redemptionInstructions: Option[String] = None
  ): SubscriptionDataExtensionRow = {


    val address = personalData.address
    val paymentDelay = Days.daysBetween(subscription.startDate, subscription.firstPaymentDate).minus(gracePeriod)

    val paymentFields = paymentMethod match {
      case com.gu.memsub.GoCardless(mandateId, accName, accNumber, sortCode) => Seq(
          "Account number" -> formatAccountNumber(accNumber),
          "Sort Code" -> formatSortCode(sortCode),
          "Account Name" -> accName,
          "Default payment method" -> "Direct Debit",
          "MandateID" -> mandateId
        )
      case _: com.gu.memsub.PaymentCard => Seq(
        "Default payment method" -> "Credit/Debit Card"
      )
    }

    val promotionFields =
      promotionDescription.map(d => "Promotion description" -> d.substring(0, Math.min(d.length, 255))) ++
        redemptionInstructions.map(i => "Redemption instructions" -> i.substring(0, Math.min(i.length, 255)))

    SubscriptionDataExtensionRow(
      personalData.email,
      Seq(
        "ZuoraSubscriberId" -> subscription.name.get,
        "SubscriberKey" -> personalData.email,
        "EmailAddress" -> personalData.email,
        "Subscription term" -> subscription.plan.billingPeriod.noun,
        "Payment amount" -> formatPrice(subscription.recurringPrice.amount),
        "First Name" -> personalData.first,
        "Last Name" -> personalData.last,
        "Address 1" -> address.lineOne,
        "Address 2" -> address.lineTwo,
        "City" -> address.town,
        "Post Code" -> address.postCode,
        "Country" -> address.country.fold(address.countryName)(_.name),
        "Date of first payment" -> formatDate(subscription.firstPaymentDate),
        "Currency" -> personalData.currency.glyph,
        "Trial period" -> paymentDelay.getDays.toString,
        "Email" -> personalData.email,
        "Subscription details" -> subscriptionDetails
      ) ++ paymentFields ++ promotionFields
    )
  }

  private def formatDate(dateTime: LocalDate) = {
    val day = dateTime.dayOfMonth.get

    val dayWithSuffix = Dates.getOrdinalDay(day)

    val month = dateTime.monthOfYear.getAsText

    val year = dateTime.year.getAsString

    s"$dayWithSuffix $month $year"
  }

  private def formatPrice(price: Float): String = {
    decimal(price).bigDecimal.stripTrailingZeros.toPlainString
  }

  private def formatAccountNumber(AccountNumber: String): String = {
    val lastFour = AccountNumber takeRight 4
    s"****$lastFour"
  }

  private def formatSortCode(sortCode: String): String =
    sortCode.filter(_.isDigit).grouped(2).mkString("-")
}

trait DataExtensionRow {
  def fields: Seq[(String, String)]
}

case class SubscriptionDataExtensionRow(email: String, fields: Seq[(String, String)]) extends DataExtensionRow
