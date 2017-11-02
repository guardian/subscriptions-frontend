package model

import com.gu.acquisition.model.{OphanIds, ReferrerAcquisitionData}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.Benefit.{PaperDay, Weekly}
import com.gu.memsub.BillingPeriod
import com.gu.memsub.BillingPeriod.{Month, Quarter, SixMonths, Year}
import com.gu.memsub.promo.{PercentDiscount, Promotion}
import com.gu.stripe.Stripe.Charge
import com.typesafe.scalalogging.LazyLogging
import forms.SubscriptionsForm
import ophan.thrift.event.PaymentProvider.{Gocardless, Stripe}
import ophan.thrift.event._
import play.api.libs.json._

//import com.gu.acquisition.model.ReferrerAcquisitionData.referrerAcquisitionDataReads

case class SubscriptionAcquisitionComponents(
  subscribeRequest: SubscribeRequest,
  promotion: Option[Promotion.AnyPromotion],
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

    private def referrerAcquisitionDataFromJSON(json: String): Option[ReferrerAcquisitionData] = {
      val parsedJson = parseJsonSafely(json)

      parsedJson
        .map(json => Json.fromJson[ReferrerAcquisitionData](json))
        .flatMap({
          case JsSuccess(referrerAcquisitionData, _) => Some(referrerAcquisitionData)
          case e: JsError => {
            logger.warn(s"Unable to decode JSON $parsedJson to an instance of ReferrerAcquisitionData. ${JsError.toJson(e).toString()}")
            None
          }
        })
    }

    private def printOptionsFromPaperData(p: PaperData): Option[PrintOptions] = {
      // Explicit imports because Product and Benefit have name collisions
      import com.gu.memsub.Product.{Delivery, Voucher, Weekly}
      import com.gu.memsub.Benefit.{MondayPaper, TuesdayPaper, WednesdayPaper, ThursdayPaper, FridayPaper, SaturdayPaper, SundayPaper, Digipack}

      val paperDays = p.plan.charges.benefits.list.filter(_ match {
        case p: PaperDay => true
        case _ => false
      })

      sealed trait When
      case object Sunday extends When
      case object Weekend extends When
      case object Sixday extends When
      case object Everyday extends When

      val when = Some(paperDays) collect {
        case List(SundayPaper) => Sunday
        case List(SaturdayPaper, SundayPaper) => Weekend
        case List(MondayPaper, TuesdayPaper, WednesdayPaper, ThursdayPaper, FridayPaper, SaturdayPaper) => Sixday
        case List(MondayPaper, TuesdayPaper, WednesdayPaper, ThursdayPaper, FridayPaper, SaturdayPaper, SundayPaper) => Everyday
      }

      val hasDigipack = p.plan.charges.benefits.list.contains(Digipack)

      val printProduct = Some(when, p.plan.product, hasDigipack) collect {
        case (Some(Sunday), Delivery, false) => PrintProduct.HomeDeliverySunday
        case (Some(Sunday), Delivery, true) => PrintProduct.HomeDeliverySundayPlus
        case (Some(Weekend), Delivery, false) => PrintProduct.HomeDeliveryWeekend
        case (Some(Weekend), Delivery, true) => PrintProduct.HomeDeliveryWeekendPlus
        case (Some(Sixday), Delivery, false) => PrintProduct.HomeDeliverySixday
        case (Some(Sixday), Delivery, true) => PrintProduct.HomeDeliverySixdayPlus
        case (Some(Everyday), Delivery, false) => PrintProduct.HomeDeliveryEveryday
        case (Some(Everyday), Delivery, true) => PrintProduct.HomeDeliveryEverydayPlus

        case (Some(Sunday), Voucher, false) => PrintProduct.VoucherSunday
        case (Some(Sunday), Voucher, true) => PrintProduct.VoucherSundayPlus
        case (Some(Weekend), Voucher, false) => PrintProduct.VoucerWeekend
        case (Some(Weekend), Voucher, true) => PrintProduct.VoucerWeekendPlus
        case (Some(Sixday), Voucher, false) => PrintProduct.VoucherSixday
        case (Some(Sixday), Voucher, true) => PrintProduct.VoucherSixdayPlus
        case (Some(Everyday), Voucher, false) => PrintProduct.VoucherEveryday
        case (Some(Everyday), Voucher, true) => PrintProduct.VoucherEverydayPlus

        case (_, _: Weekly, false) => PrintProduct.GuardianWeekly
        case (_, _: Weekly, true) => PrintProduct.GuardianWeeklyPlus
      }

      for {
        d <- p.deliveryAddress.country.map(_.alpha2)
        p <- printProduct
      } yield PrintOptions(p, d)
    }

    override def buildAcquisition(components: SubscriptionAcquisitionComponents): Either[String, Acquisition] = {
      import components._

      val plan = subscribeRequest.productData.fold(_.plan, _.plan)
      val referrerAcquisitionData = acquisitionDataJSON.flatMap(referrerAcquisitionDataFromJSON)

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

          printOptions = subscribeRequest.productData match {
            case Left(paperData) => printOptionsFromPaperData(paperData)
            case _ => None
          },

          discountPercentage = promotion.map(_.promotionType) collect {
            case PercentDiscount(_, amount) => amount
          },

          // Currently we only have discountLengthInDays.
          // It should have been discountLengthInMonths instead.
          // Once the Thrift definition is updated, we can uncomment this.
//          discountLengthInMonths = promotion.map(_.promotionType) collect {
//            case PercentDiscount(Some(months), _) => months.toShort
//          },

          campaignCode = referrerAcquisitionData.flatMap(_.campaignCode.map(Set(_))),
          abTests = referrerAcquisitionData.flatMap(_.abTest.map(ab => AbTestInfo(Set(ab)))),
          referrerPageViewId = referrerAcquisitionData.flatMap(_.referrerPageviewId),
          referrerUrl = referrerAcquisitionData.flatMap(_.referrerUrl),
          componentId = referrerAcquisitionData.flatMap(_.componentId),
          componentTypeV2 = referrerAcquisitionData.flatMap(_.componentType),
          source = referrerAcquisitionData.flatMap(_.source)
        )
      )
    }
  }
}
