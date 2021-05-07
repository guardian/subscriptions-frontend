package com.gu.acquisition.services

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.gu.acquisition.model._
import com.gu.acquisition.model.errors.AnalyticsServiceError.BuildError
import com.gu.acquisition.services.HttpAnalyticsService.RequestData
import com.gu.acquisition.services.GAService.clientIdPattern
import com.gu.acquisitionsValueCalculatorClient.model.{AcquisitionModel, PrintOptionsModel}
import com.gu.acquisitionsValueCalculatorClient.service.AnnualisedValueService
import com.typesafe.scalalogging.LazyLogging
import okhttp3._
import ophan.thrift.event.PrintProduct.{GuardianWeekly, HomeDeliveryEveryday, HomeDeliveryEverydayPlus, HomeDeliverySaturday, HomeDeliverySaturdayPlus, HomeDeliverySixday, HomeDeliverySixdayPlus, HomeDeliverySunday, HomeDeliverySundayPlus, HomeDeliveryWeekend, HomeDeliveryWeekendPlus, VoucherEveryday, VoucherEverydayPlus, VoucherSaturday, VoucherSaturdayPlus, VoucherSixday, VoucherSixdayPlus, VoucherSunday, VoucherSundayPlus, VoucherWeekend, VoucherWeekendPlus}
import ophan.thrift.event.Product.{Contribution, DigitalSubscription, PrintSubscription, RecurringContribution}
import ophan.thrift.event.{AbTestInfo, Acquisition, PrintOptions, Product}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

private[services] object GAService {
  val clientIdPattern: Regex = raw"GA\d\.\d\.(\d+\.\d+)".r
}

private[services] class GAService(implicit client: OkHttpClient)
  extends HttpAnalyticsService with LazyLogging {

  private val gaPropertyId: String = "UA-51507017-5"
  private val endpoint: HttpUrl = HttpUrl.parse("https://www.google-analytics.com")

  private[services] def buildBody(submission: AcquisitionSubmission)(implicit ec: ExecutionContext): EitherT[Future, BuildError, RequestBody] = EitherT {
    getAnnualisedValue(submission.acquisition)
      .fold(
        error => {
          logger.warn(s"Couldn't retrieve annualised value for this acquisition: $error")
          buildPayload(submission, 0) // We still want to record the acquisition even if we can't get the AV
        },
        value => buildPayload(submission, value)
      ).map {
      maybePayload =>
        maybePayload.map { payload =>
          logger.debug(s"GA payload: $payload")
          RequestBody.create(null, payload)
        }
    }
  }

  private def getAnnualisedValue(acquisition: Acquisition)(implicit ec: ExecutionContext) = {
    val acquisitionModel = AcquisitionModel(acquisition.amount,
      acquisition.product.originalName,
      acquisition.currency,
      acquisition.paymentFrequency.originalName,
      acquisition.paymentProvider.map(_.toString),
      acquisition.printOptions.map(printOptions => PrintOptionsModel(printOptions.product.originalName, printOptions.deliveryCountryCode))
    )

    EitherT(AnnualisedValueService.getAsyncAV(acquisitionModel, "ophan"))
  }

  private def getSuccessfulSubscriptionSignUpMetric(conversionCategory: ConversionCategory) =
    conversionCategory match {
      case _: ConversionCategory.ContributionConversion.type => ""
      case _ => "1"
    }

  private[services] def buildPayload(submission: AcquisitionSubmission, annualisedValue: Double, transactionId: Option[String] = None): Either[BuildError, String] = {
    import submission._
    val tid = transactionId.getOrElse(UUID.randomUUID().toString)
    val goExp = buildOptimizeTestsPayload(acquisition.abTests)

    // clientId cannot be empty or the call will fail
    sanitiseClientId(gaData.clientId).map {
      clientId =>
        val productName = getProductName(submission.acquisition)
        val conversionCategory = getConversionCategory(submission.acquisition)
        val productCheckout = getProductCheckout(submission.acquisition)
        val body = Map(
          "v" -> "1", //Version
          "cid" -> clientId,
          "tid" -> gaPropertyId,
          "dh" -> gaData.hostname,
          "uip" -> gaData.clientIpAddress.getOrElse(""), // IP Override
          "ua" -> gaData.clientUserAgent.getOrElse(""), // User Agent Override

          // Custom Dimensions
          "cd12" -> acquisition.campaignCode.map(_.mkString(",")).getOrElse(""), // Campaign code
          "cd16" -> buildABTestPayload(acquisition.abTests), //'Experience' custom dimension
          "cd17" -> acquisition.paymentProvider.getOrElse(""), // Payment method
          "cd19" -> acquisition.promoCode.getOrElse(""), // Promo code
          "cd25" -> acquisition.labels.exists(_.contains("REUSED_EXISTING_PAYMENT_METHOD")), // usedExistingPaymentMethod
          "cd26" -> acquisition.labels.exists(_.contains("gift-subscription")), // gift subscription
          "cd27" -> productCheckout,
          "cd30" -> acquisition.labels.exists(_.contains("corporate-subscription")), // corporate subscription

          // Custom metrics
          "cm10" -> getSuccessfulSubscriptionSignUpMetric(conversionCategory),

          // Google Optimize Experiment Id
          "xid" -> goExp.map(_._1).getOrElse(""),
          "xvar" -> goExp.map(_._2).getOrElse(""),

          // The GA conversion event
          "t" -> "event",
          "ec" -> conversionCategory.name, //Event Category
          "ea" -> productName, //Event Action
          "el" -> acquisition.paymentFrequency.name, //Event Label
          "ev" -> acquisition.amount.toInt.toString, //Event Value is an Integer

          // Enhanced Ecommerce tracking https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#enhancedecom
          "ti" -> tid,
          "tcc" -> acquisition.promoCode.getOrElse(""), // Transaction coupon.
          "pa" -> "purchase", //This is a purchase
          "pr1nm" -> productName, // Product Name
          "pr1ca" -> conversionCategory.description, // Product category
          "pr1pr" -> annualisedValue, // Product Price - we are tracking the annualised value here as this is what goes into the revenue metric
          "pr1qt" -> "1", // Product Quantity
          "pr1cc" -> acquisition.promoCode.getOrElse(""), // Product coupon code.
          "pr1cm15" -> acquisition.amount.toString, // Custom metric 15 - purchasePrice
          "cu" -> acquisition.currency.toString // Currency
        )

        body
          .filter { case (key, value) => value != "" }
          .map { case (key, value) => s"$key=$value" }
          .mkString("&")
    }
  }

  private[services] def sanitiseClientId(maybeId: String): Either[BuildError, String] = {
    maybeId match {
      case clientIdPattern(id) => Right(id) // If we have a full _ga cookie string extract the client id
      case "" => Left(BuildError("Client ID cannot be an empty string.\n" +
        "To link server side with client side events you need to pass a valid clientId from the `_ga` cookie.\n" +
        "Otherwise you can pass any non-empty string eg. `UUID.randomUUID().toString`\n" +
        "More info here: https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#cid"))
      case _ => Right(maybeId) // Otherwise assume that the caller has passed in the client id correctly
    }
  }

  private[services] def getProductCheckout(acquisition: Acquisition) =
    acquisition.product match {
      case Contribution | RecurringContribution => Some("Contribution")
      case DigitalSubscription => Some("DigitalPack")
      case PrintSubscription => getProductCheckoutForPrint(acquisition.printOptions)
      case default => None
    }

  private def getProductCheckoutForPrint(maybePrintOptions: Option[PrintOptions]) =
    maybePrintOptions.map(
      _.product match {
        case VoucherEveryday | VoucherSaturday | VoucherSunday | VoucherSixday | VoucherWeekend |
             HomeDeliveryEveryday | HomeDeliverySaturday | HomeDeliverySunday | HomeDeliverySixday | HomeDeliveryWeekend
        => "Paper"
        case VoucherEverydayPlus | VoucherSaturdayPlus | VoucherSundayPlus | VoucherSixdayPlus | VoucherWeekendPlus |
             HomeDeliveryEverydayPlus | HomeDeliverySaturdayPlus | HomeDeliverySundayPlus | HomeDeliverySixdayPlus | HomeDeliveryWeekendPlus
        => "PaperAndDigital"
        case GuardianWeekly => "GuardianWeekly"
      }
    )

  private[services] def getProductName(acquisition: Acquisition) =
    acquisition.printOptions.map(_.product.name).getOrElse(acquisition.product.name)

  private[services] def getConversionCategory(acquisition: Acquisition) =
    acquisition.printOptions.map(p => ConversionCategory.PrintConversion)
      .getOrElse(getDigitalConversionCategory(acquisition))

  private[services] def getDigitalConversionCategory(acquisition: Acquisition) =
    acquisition.product match {
      case _: Product.RecurringContribution.type |
           _: Product.Contribution.type => ConversionCategory.ContributionConversion
      case _ => ConversionCategory.DigitalConversion
    }

  private[services] def buildOptimizeTestsPayload(maybeTests: Option[AbTestInfo]) = {
    val optimizePrefix = "optimize$$"
    maybeTests.map {
      abTests =>
        abTests.tests
          .filter(test => test._1.startsWith(optimizePrefix))
          .map(test => test._1.replace(optimizePrefix, "") -> test._2)
          .toMap
    }.map(tests => (tests.keys.mkString(","), tests.values.mkString(",")))

  }

  private[services] def buildABTestPayload(maybeTests: Option[AbTestInfo]) =
    maybeTests.map {
      abTests =>
        abTests.tests
          .map(test => s"${test._1}=${test._2}")
          .mkString(",")
    }.getOrElse("")


  override def buildRequest(submission: AcquisitionSubmission)(implicit ec: ExecutionContext): EitherT[Future, BuildError, RequestData] = {

    val url = endpoint.newBuilder()
      .addPathSegment("collect")
      .build()

    buildBody(submission).map {
      requestBody =>
        val request = new Request.Builder()
          .url(url)
          .addHeader("User-Agent", submission.gaData.clientUserAgent.getOrElse(""))
          .post(requestBody)
          .build()

        RequestData(request, submission)
    }
  }
}
