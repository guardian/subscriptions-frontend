package model

import com.gu.acquisition.model.{OphanIds, ReferrerAcquisitionData}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.BillingPeriod
import com.gu.memsub.BillingPeriod.{Month, Quarter, SixMonths, Year}
import com.gu.stripe.Stripe.Charge
import com.typesafe.scalalogging.LazyLogging
import forms.SubscriptionsForm
import ophan.thrift.event.PaymentProvider.{Gocardless, Stripe}
import ophan.thrift.event._
import play.api.libs.json._

//import com.gu.acquisition.model.ReferrerAcquisitionData.referrerAcquisitionDataReads

case class SubscriptionAcquisitionComponents(
  subscribeRequest: SubscribeRequest,
  acquisitionDataJSON: Option[String]
)


object SubscriptionAcquisitionComponents {

  implicit object subscriptionAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[SubscriptionAcquisitionComponents] with LazyLogging {

    def buildOphanIds(components: SubscriptionAcquisitionComponents): Either[String, OphanIds] = {
      import components._

      Right(
        OphanIds(
          pageviewId = subscribeRequest.genericData.ophanPageViewId,
          visitId = subscribeRequest.genericData.ophanVisitId,
          browserId = subscribeRequest.genericData.ophanBrowserId
        )
      )
    }

    private def parseJsonSafely(json: String): Option[JsValue] = {
      try {
        Some(Json.parse(json))
      } catch {
        case scala.util.control.NonFatal(t) =>
          logger.warn(s"""Unable to parse "$json" as JSON. ${t.getMessage()}""")
          None
      }
    }

    override def buildAcquisition(components: SubscriptionAcquisitionComponents): Either[String, Acquisition] = {
      import components._

      val plan = subscribeRequest.productData.fold(_.plan, _.plan)
      val parsedJson = acquisitionDataJSON.flatMap(parseJsonSafely)
      val acquisitionData = parsedJson
        .map(json => Json.fromJson[ReferrerAcquisitionData](json))
        .flatMap({
          case JsSuccess(referrerAcquisitionData, _) => Some(referrerAcquisitionData)
          case e: JsError => {
            logger.warn(s"Unable to decode JSON $parsedJson to an instance of ReferrerAcquisitionData. ${JsError.toJson(e).toString()}")
            None
          }
        })

      Right(
        Acquisition(
          product = subscribeRequest.productData match {
            case Left(_) => Product.PrintSubscription
            case Right(_) => Product.DigitalSubscription
          },

          paymentFrequency = plan.charges.billingPeriod match {
            case Month => PaymentFrequency.Monthly
            case Quarter => PaymentFrequency.Quarterly
            case SixMonths => PaymentFrequency.SixMonthly
            case Year => PaymentFrequency.Annually
          },

          currency = subscribeRequest.genericData.currency.iso,

          amount = plan.charges.price.getPrice(subscribeRequest.genericData.currency)
            .map(_.amount.toDouble).getOrElse(0),

          paymentProvider = subscribeRequest.genericData.paymentData match {
            case DirectDebitData(_, _, _) => Some(Gocardless)
            case CreditCardData(_) => Some(Stripe)
            case _ => None
          },

          countryCode = subscribeRequest.genericData.personalData.address.country.map(_.alpha2),

          // printOptions
          // discountLengthInDays
          // discountPercentage

          // TODO: platform Thrift definition!

          campaignCode = acquisitionData.flatMap(_.campaignCode.map(Set(_))),
          abTests = acquisitionData.flatMap(_.abTest.map(ab => AbTestInfo(Set(ab)))),
          referrerPageViewId = acquisitionData.flatMap(_.referrerPageviewId),
          referrerUrl = acquisitionData.flatMap(_.referrerUrl),
          componentId = acquisitionData.flatMap(_.componentId),
          componentTypeV2 = acquisitionData.flatMap(_.componentType),
          source = acquisitionData.flatMap(_.source)
        )
      )
    }
  }
}
