package model

import java.lang.Math.min
import java.util.UUID

import com.gu.exacttarget._
import com.gu.i18n.{Currency, Title}
import com.gu.memsub
import com.gu.memsub.Benefit.Digipack
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan => Plan}
import com.gu.memsub.{Subscription => _, _}
import com.gu.salesforce.Contact
import com.typesafe.scalalogging.LazyLogging
import model.EmailHelpers._
import org.joda.time.Days.daysBetween
import org.joda.time.{Days, LocalDate}
import views.support.Dates.prettyDate

import scala.math.BigDecimal.decimal
import scalaz.NonEmptyList
import scalaz.syntax.nel._
import scalaz.syntax.std.list._

trait Email {
  def email: String
  def fields: Seq[(String, String)]
  def forExtension: DataExtension
}

object EmailHelpers {
  val dd = "Direct Debit"
  val card = "Credit/Debit Card"
  val paypal = "PayPal"

  def formatDate(dateTime: LocalDate) = {
    val day = dateTime.dayOfMonth.get

    val month = dateTime.monthOfYear.getAsText

    val year = dateTime.year.getAsString

    s"$day $month $year"
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

object DigipackWelcome1Email extends LazyLogging {

  def apply(
             personalData: PersonalData,
             subscription: Subscription[Plan.Paid],
             paymentMethod: PaymentMethod,
             gracePeriod: Days,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None,
             currency:Currency
           ): DigipackWelcome1Email = {

    val paymentDelay = daysBetween(subscription.startDate, subscription.firstPaymentDate).minus(gracePeriod)

    val paymentFields = paymentMethod match {
      case GoCardless(mandateId, accName, accNumber, sortCode, _, _) => Seq(
        "Account number" -> formatAccountNumber(accNumber),
        "Sort Code" -> formatSortCode(sortCode),
        "Account Name" -> accName,
        "Default payment method" -> dd,
        "MandateID" -> mandateId
      )
      case _: PaymentCard => Seq("Default payment method" -> card)
      case _ => Seq()
    }

    val promotionFields = promotionDescription.map(d => "Promotion description" -> trimPromotionDescription(d))

    DigipackWelcome1Email(
      personalData.email,
      Seq(
        "ZuoraSubscriberId" -> subscription.name.get,
        "SubscriberKey" -> personalData.email,
        "EmailAddress" -> personalData.email,
        "Subscription term" -> subscription.plan.charges.billingPeriod.noun,
        "Payment amount" -> formatPrice(subscription.plan.charges.price.prices.head.amount),
        "First Name" -> personalData.first,
        "Last Name" -> personalData.last,
        "Address 1" -> personalData.address.lineOne,
        "Address 2" -> personalData.address.lineTwo,
        "City" -> personalData.address.town,
        "Post Code" -> personalData.address.postCode,
        "Country" -> personalData.address.country.fold(personalData.address.countryName)(_.name),
        "Date of first payment" -> formatDate(subscription.firstPaymentDate),
        "Currency" -> currency.glyph,
        "Trial period" -> paymentDelay.getDays.toString,
        "Subscription details" -> subscriptionDetails
      ) ++ paymentFields ++ promotionFields
    )
  }


}

object PaperFieldsGenerator {

  def fieldsFor(
                 paperData: PaperData,
                 personalData: PersonalData,
                 subscription: Subscription[Plan.Paid],
                 paymentMethod: PaymentMethod,
                 subscriptionDetails: String,
                 promotionDescription: Option[String] = None
               ): Seq[(String, String)] = {

    fieldsFor(
      deliveryAddress = paperData.deliveryRecipient.address,
      maybeBillingAddress = Some(personalData.address),
      includesDigipack = paperData.plan.charges.benefits.list.toList.contains(Digipack),
      email = personalData.email,
      title = personalData.title,
      firstName = personalData.first,
      lastName = personalData.last,
      startDate= paperData.startDate,
      firstPaymentDate = subscription.firstPaymentDate,
      planName = paperData.plan.name,
      subscriptionName = subscription.name,
      paymentMethod = paymentMethod,
      subscriptionDetails = subscriptionDetails,
      promotionDescription = promotionDescription
    )
  }

  def fieldsFor(
                 deliveryAddress: Address,
                 maybeBillingAddress: Option[Address],
                 includesDigipack: Boolean,
                 email: String,
                 title: Option[Title],
                 firstName: String,
                 lastName: String,
                 startDate: LocalDate,
                 firstPaymentDate: LocalDate,
                 planName : String,
                 subscriptionName: memsub.Subscription.Name,
                 paymentMethod: PaymentMethod,
                 subscriptionDetails: String,
                 promotionDescription: Option[String]
               ): Seq[(String, String)] = {
    val paymentFields = paymentMethod match {
      case GoCardless(mandateId, accName, accNumber, sortCode, _, _) => Seq(
        "bank_account_no" -> formatAccountNumber(accNumber),
        "bank_sort_code" -> formatSortCode(sortCode),
        "account_holder" -> accName,
        "payment_method" -> dd,
        "mandate_id" -> mandateId
      )
      case _: PaymentCard => Seq("payment_method" -> card)
      case _: PayPalMethod => Seq("payment_method" -> paypal)
    }

    val promotionFields = promotionDescription.map(d => "promotion_details" -> trimPromotionDescription(d))

    val billingAddressFields = maybeBillingAddress.map( billingAddress =>
      if (billingAddress != deliveryAddress) Seq(
        "billing_address_line_1" -> billingAddress.lineOne,
        "billing_address_line_2" -> billingAddress.lineTwo,
        "billing_address_town" -> billingAddress.town,
        "billing_county" -> billingAddress.countyOrState,
        "billing_postcode" -> billingAddress.postCode,
        "billing_country" -> billingAddress.country.fold(billingAddress.countryName)(_.name)
      )
      else Seq.empty
    ).getOrElse(Seq.empty)

    Seq(
      "ZuoraSubscriberId" -> subscriptionName.get,
      "SubscriberKey" -> email,
      "EmailAddress" -> email,
      "subscriber_id" -> subscriptionName.get,
      "IncludesDigipack" -> includesDigipack.toString,
      "title" -> title.fold("")(_.title),
      "first_name" -> firstName,
      "last_name" -> lastName,
      "delivery_address_line_1" -> deliveryAddress.lineOne,
      "delivery_address_line_2" -> deliveryAddress.lineTwo,
      "delivery_address_town" -> deliveryAddress.town,
      "delivery_county" -> deliveryAddress.countyOrState,
      "delivery_postcode" -> deliveryAddress.postCode,
      "delivery_country" -> deliveryAddress.country.fold(deliveryAddress.countryName)(_.name),
      "date_of_first_paper" -> formatDate(startDate),
      "date_of_first_payment" -> formatDate(firstPaymentDate),
      "package" -> planName,
      "subscription_rate" -> subscriptionDetails
    ) ++ billingAddressFields ++ paymentFields ++ promotionFields

  }
}

object PaperHomeDeliveryWelcome1Email {
  def apply(
             paperData: PaperData,
             personalData: PersonalData,
             subscription: Subscription[Plan.Paid],
             paymentMethod: PaymentMethod,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None
           ): PaperHomeDeliveryWelcome1Email = {

    val commonFields = PaperFieldsGenerator.fieldsFor(paperData, personalData, subscription, paymentMethod, subscriptionDetails, promotionDescription)
    val additionalFields = Seq("delivery_instructions" -> paperData.deliveryInstructions.getOrElse(""))

    PaperHomeDeliveryWelcome1Email(personalData.email, commonFields ++ additionalFields)

  }
}

object PaperVoucherWelcome1Email {
  def apply(
             paperData: PaperData,
             personalData: PersonalData,
             subscription: Subscription[Plan.Paid],
             paymentMethod: PaymentMethod,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None
           ): PaperVoucherWelcome1Email = {

    val fields = PaperFieldsGenerator.fieldsFor(paperData, personalData, subscription, paymentMethod, subscriptionDetails, promotionDescription)
    PaperVoucherWelcome1Email(personalData.email, fields)
  }
}

object GuardianWeeklyWelcome1Email {

  import model.SubscriptionOps._

  def apply(
             paperData: PaperData,
             personalData: PersonalData,
             subscription: Subscription[Plan.Paid],
             paymentMethod: PaymentMethod,
             subscriptionDetails: String,
             promotionDescription: Option[String] = None
           ): GuardianWeeklyWelcome1Email = {

    val commonFields = PaperFieldsGenerator.fieldsFor(paperData, personalData, subscription, paymentMethod, subscriptionDetails, promotionDescription)

    val additionalFields = subscription.asWeekly.map { weeklySub =>
      Seq("date_of_second_payment" -> formatDate(weeklySub.secondPaymentDate))
    } getOrElse Nil

    GuardianWeeklyWelcome1Email(personalData.email, commonFields ++ additionalFields)
  }
}

object HolidaySuspensionBillingScheduleEmail {

  def apply(
           email: Option[String],
           saluation: String,
           subscriptionName: String,
           subscriptionCurrency: Currency,
           packageName: String,
           billingSchedule: BillingSchedule,
           numberOfSuspensionsLinedUp: Int,
           daysUsed: Int,
           daysAllowed: Int
         ): HolidaySuspensionBillingScheduleEmail = {

    val (thereafterBill, trimmedSchedule) = BillingSchedule.rolledUp(billingSchedule)
    val schedule = trimmedSchedule.toList.toNel.getOrElse(thereafterBill.wrapNel)
    val futureBills = schedule.zipWithIndex.flatMap { case (bill, number) => NonEmptyList(
        s"future_bill_${number + 1}_date" -> prettyDate(bill.date),
        s"future_bill_${number + 1}_amount" -> Price(bill.amount, subscriptionCurrency).pretty
      )
    }

    val emailAddress = email.getOrElse("holiday-suspension-bounce@guardian.co.uk")

    HolidaySuspensionBillingScheduleEmail(
      emailAddress,
      subscriptionName,
      Seq(
        "ZuoraSubscriberId" -> subscriptionName,
        "SubscriberKey" -> emailAddress,
        "EmailAddress" -> emailAddress,
        "uuid" -> UUID.randomUUID().toString,
        "subscriber_id" -> subscriptionName,
        "customer_salutation" -> saluation,
        "package_name" -> packageName,
        "days_allowed" -> daysAllowed.toString,
        "days_used" -> daysUsed.toString,
        "days_remaining" -> (daysAllowed - daysUsed).toString,
        "number_of_suspensions_lined_up" -> numberOfSuspensionsLinedUp.toString,
        "normal_price" -> Price(thereafterBill.amount, subscriptionCurrency).pretty
      ) ++ futureBills.list.toList
    )
  }

  def constructSalutation(titleOpt: Option[String], firstNameOpt: Option[String], lastNameOpt: Option[String]): String =
    titleOpt.flatMap(title => lastNameOpt.map(lastName => s"$title $lastName")).getOrElse(firstNameOpt.getOrElse("Subscriber"))
}



object GuardianWeeklyRenewalEmail {

  private def extractAddress(contact: Contact) = Address(
    lineOne = contact.mailingStreet.getOrElse(""),
    lineTwo = "",
    town = contact.mailingCity.getOrElse(""),
    countyOrState = contact.mailingState.getOrElse(""),
    postCode = contact.mailingPostcode.getOrElse(""),
    countryName = contact.mailingCountry.getOrElse("")
  )

  def apply(subscriptionName: memsub.Subscription.Name,
            subscriptionDetails: String,
            planName: String,
            contact: Contact,
            paymentMethod: PaymentMethod,
            email: String,
            newTermStartDate: LocalDate
           ): GuardianWeeklyRenewalEmail = {


    val commonFields = PaperFieldsGenerator.fieldsFor(
      deliveryAddress = extractAddress(contact),
      maybeBillingAddress = None,
      includesDigipack = false,
      email = email,
      title = contact.title.flatMap(Title.fromString),
      firstName = contact.firstName.getOrElse(""),
      lastName = contact.lastName,
      startDate = newTermStartDate,
      firstPaymentDate = newTermStartDate,
      planName = planName,
      subscriptionName = subscriptionName,
      paymentMethod = paymentMethod,
      subscriptionDetails = subscriptionDetails,
      promotionDescription = None
    )

    GuardianWeeklyRenewalEmail(email, commonFields)
  }

  def constructSalutation(titleOpt: Option[String], firstNameOpt: Option[String], lastNameOpt: Option[String]): String =
    titleOpt.flatMap(title => lastNameOpt.map(lastName => s"$title $lastName")).getOrElse(firstNameOpt.getOrElse("Subscriber"))
}

case class DigipackWelcome1Email(email: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = DigipackDataExtension
}

case class PaperHomeDeliveryWelcome1Email(email: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = PaperDeliveryDataExtension
}

case class PaperVoucherWelcome1Email(email: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = PaperVoucherDataExtension
}
case class GuardianWeeklyWelcome1Email(email: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = GuardianWeeklyWelcome1DataExtension
}

case class HolidaySuspensionBillingScheduleEmail(email: String, subscriptionName: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = HolidaySuspensionBillingScheduleExtension
}
case class GuardianWeeklyRenewalEmail(email: String, fields: Seq[(String, String)]) extends Email {
  override def forExtension = GuardianWeeklyRenewalDataExtension
}
