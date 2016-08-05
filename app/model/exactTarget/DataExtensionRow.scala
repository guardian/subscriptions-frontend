package model.exactTarget

import java.lang.Math.min

import com.gu.memsub._
import com.gu.exacttarget._
import com.gu.memsub.Subscription._
import com.typesafe.scalalogging.LazyLogging
import model.exactTarget.DataExtensionRowHelpers._
import org.joda.time.{Days, LocalDate}
import model.{PaperData, PersonalData}
import org.joda.time.Days.daysBetween
import utils.Dates

import scala.math.BigDecimal.decimal

trait DataExtensionRow {
  def email: String
  def fields: Seq[(String, String)]
  def forExtension: DataExtension
}

object DataExtensionRowHelpers {
  val dd = "Direct Debit"
  val card = "Credit/Debit Card"

  def formatDate(dateTime: LocalDate) = {
    val day = dateTime.dayOfMonth.get

    val dayWithSuffix = Dates.getOrdinalDay(day)

    val month = dateTime.monthOfYear.getAsText

    val year = dateTime.year.getAsString

    s"$dayWithSuffix $month $year"
  }

  def formatPrice(price: Float): String = {
    decimal(price).bigDecimal.stripTrailingZeros.toPlainString
  }

  def formatAccountNumber(AccountNumber: String): String = {
    val lastFour = AccountNumber takeRight 4
    s"****$lastFour"
  }

  def formatSortCode(sortCode: String): String =
    sortCode.filter(_.isDigit).grouped(2).mkString("-")

  def trimPromotionDescription(p: String): String = p.substring(0, min(p.length, 255))
}

object DigipackWelcome1DataExtensionRow extends LazyLogging {

  def apply(
             personalData: PersonalData,
             subscription: Subscription with Paid,
             paymentMethod: PaymentMethod,
             gracePeriod: Days,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None
           ): DigipackWelcome1DataExtensionRow = {

    val paymentDelay = daysBetween(subscription.startDate, subscription.firstPaymentDate).minus(gracePeriod)

    val paymentFields = paymentMethod match {
      case GoCardless(mandateId, accName, accNumber, sortCode) => Seq(
        "Account number" -> formatAccountNumber(accNumber),
        "Sort Code" -> formatSortCode(sortCode),
        "Account Name" -> accName,
        "Default payment method" -> dd,
        "MandateID" -> mandateId
      )
      case _: PaymentCard => Seq("Default payment method" -> card)
    }

    val promotionFields = promotionDescription.map(d => "Promotion description" -> trimPromotionDescription(d))

    DigipackWelcome1DataExtensionRow(
      personalData.email,
      Seq(
        "ZuoraSubscriberId" -> subscription.name.get,
        "SubscriberKey" -> personalData.email,
        "EmailAddress" -> personalData.email,
        "Subscription term" -> subscription.plan.billingPeriod.noun,
        "Payment amount" -> formatPrice(subscription.recurringPrice.amount),
        "First Name" -> personalData.first,
        "Last Name" -> personalData.last,
        "Address 1" -> personalData.address.lineOne,
        "Address 2" -> personalData.address.lineTwo,
        "City" -> personalData.address.town,
        "Post Code" -> personalData.address.postCode,
        "Country" -> personalData.address.country.fold(personalData.address.countryName)(_.name),
        "Date of first payment" -> formatDate(subscription.firstPaymentDate),
        "Currency" -> personalData.currency.glyph,
        "Trial period" -> paymentDelay.getDays.toString,
        "Subscription details" -> subscriptionDetails
      ) ++ paymentFields ++ promotionFields
    )
  }


}

object PaperHomeDeliveryWelcome1DataExtensionRow {
  def apply(
             paperData: PaperData,
             personalData: PersonalData,
             subscription: Subscription with Paid,
             paymentMethod: PaymentMethod,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None
           ): PaperHomeDeliveryWelcome1DataExtensionRow = {

    val paymentFields = paymentMethod match {
      case GoCardless(mandateId, accName, accNumber, sortCode) => Seq(
        "bank_account_no" -> formatAccountNumber(accNumber),
        "bank_sort_code" -> formatSortCode(sortCode),
        "account_holder" -> accName,
        "payment_method" -> dd,
        "mandate_id" -> mandateId
      )
      case _: PaymentCard => Seq("payment_method" -> card)
    }

    val promotionFields = promotionDescription.map(d => "promotion_details" -> trimPromotionDescription(d))

    val billingAddressFields =
      if (!personalData.address.equals(paperData.deliveryAddress)) Seq(
        "billing_address_line_1" -> personalData.address.lineOne,
        "billing_address_line_2" -> personalData.address.lineTwo,
        "billing_address_town" -> personalData.address.town,
        "billing_county" -> personalData.address.countyOrState,
        "billing_postcode" -> personalData.address.postCode,
        "billing_country" -> personalData.address.country.fold(personalData.address.countryName)(_.name)
      ) else Seq.empty

    PaperHomeDeliveryWelcome1DataExtensionRow(
      personalData.email,
      Seq(
        "ZuoraSubscriberId" -> subscription.name.get,
        "SubscriberKey" -> personalData.email,
        "EmailAddress" -> personalData.email,
        "subscriber_id" -> subscription.name.get,
        "IncludesDigipack" -> paperData.plan.products.others.map(_._1).contains(Digipack).toString,
        "title" -> personalData.title.fold("")(_.title),
        "first_name" -> personalData.first,
        "last_name" -> personalData.last,
        "delivery_address_line_1" -> paperData.deliveryAddress.lineOne,
        "delivery_address_line_2" -> paperData.deliveryAddress.lineTwo,
        "delivery_address_town" -> paperData.deliveryAddress.town,
        "delivery_county" -> paperData.deliveryAddress.countyOrState,
        "delivery_postcode" -> paperData.deliveryAddress.postCode,
        "delivery_country" -> paperData.deliveryAddress.country.fold(paperData.deliveryAddress.countryName)(_.name),
        "delivery_instructions" -> paperData.deliveryInstructions.getOrElse(""),
        "date_of_first_paper" -> formatDate(paperData.startDate),
        "date_of_first_payment" -> formatDate(subscription.firstPaymentDate),
        "package" -> paperData.plan.name,
        "subscription_rate" -> subscriptionDetails
      ) ++ billingAddressFields ++ paymentFields ++ promotionFields
    )
  }
}

case class DigipackWelcome1DataExtensionRow(email: String, fields: Seq[(String, String)]) extends DataExtensionRow {
  override def forExtension = DigipackDataExtension
}

case class PaperHomeDeliveryWelcome1DataExtensionRow(email: String, fields: Seq[(String, String)]) extends DataExtensionRow {
  override def forExtension = PaperDeliveryDataExtension
}
