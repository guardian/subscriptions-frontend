package model

import com.gu.acquisition.model.{OphanIds, ReferrerAcquisitionData}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.Benefit.{PaperDay, Weekly}
import com.gu.memsub.BillingPeriod
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.promo.{PercentDiscount, Promotion}
import com.gu.stripe.Stripe.Charge
import com.typesafe.scalalogging.LazyLogging
import forms.SubscriptionsForm
import ophan.thrift.event.PaymentProvider.{Gocardless, Stripe}
import ophan.thrift.event._
import play.api.libs.json._

import scala.collection.Set
import scalaz.\/

case class SubscriptionAcquisitionComponents(
  subscribeRequest: SubscribeRequest,
  promotion: Option[Promotion.AnyPromotion],
  acquisitionDataJSON: Option[String]
)


object SubscriptionAcquisitionComponents {

  sealed trait When
  case object Sunday extends When
  case object Weekend extends When
  case object Sixday extends When
  case object Everyday extends When

  implicit object subscriptionAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[SubscriptionAcquisitionComponents] with LazyLogging {

    def buildOphanIds(components: SubscriptionAcquisitionComponents): Either[String, OphanIds] = {
      import components.subscribeRequest.genericData.ophanData._

      Right(OphanIds(pageViewId, visitId, browserId))
    }

    private def referrerAcquisitionDataFromJSON(json: String): Option[ReferrerAcquisitionData] = {
      import \/._

      fromTryCatchNonFatal(Json.parse(json))
        .leftMap(err => s"""Unable to parse "$json" as JSON. $err""")
        .flatMap { jsValue =>
          Json.fromJson[ReferrerAcquisitionData](jsValue).fold(
            errs => left(logger.warn(s"Unable to decode JSON $jsValue to an instance of ReferrerAcquisitionData. ${JsError.toJson(errs)}")),
            referrerAcquisitionData => right(referrerAcquisitionData)
          )
        }
        .toOption
    }

    private def printOptionsFromPaperData(p: PaperData): Option[PrintOptions] = {
      // Explicit imports because Product and Benefit have name collisions
      import com.gu.memsub.Product.{Delivery, Voucher, Weekly}
      import com.gu.memsub.Benefit.{MondayPaper, TuesdayPaper, WednesdayPaper, ThursdayPaper, FridayPaper, SaturdayPaper, SundayPaper, Digipack}

      val paperDays = p.plan.charges.benefits.list.collect {
        case p: PaperDay => p
      }

      val when = Some(paperDays.sortBy(_.dayOfTheWeekIndex)) collect {
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
      }

      val printOptions = for {
        d <- p.deliveryAddress.country.map(_.alpha2)
        p <- printProduct
      } yield PrintOptions(p, d)

      if (printOptions.isEmpty) {
        logger.error("Could not determine PrintOptions from PaperData")
      }

      printOptions
    }

    def getAbTests(referrerAcquisitionData: Option[ReferrerAcquisitionData]): Option[AbTestInfo] =
      referrerAcquisitionData.flatMap { r =>
       r.abTests match {
         case Some(tests) => Some(AbTestInfo(tests))
         case None => r.abTest.map(abTest => AbTestInfo(Set(abTest)))
       }
     }

    override def buildAcquisition(components: SubscriptionAcquisitionComponents): Either[String, Acquisition] = {
      import components._
      import components.subscribeRequest.productData
      import components.subscribeRequest.genericData.{currency, paymentData, personalData, promoCode}

      val plan = productData.fold(_.plan, _.plan)
      val referrerAcquisitionData = acquisitionDataJSON.flatMap(referrerAcquisitionDataFromJSON)

      val product = productData match {
        case Left(_) => Product.PrintSubscription
        case Right(_) => Product.DigitalSubscription
      }

      val isSixForSix = plan.charges.billingPeriod match {
        // SixWeeks is a special case.
        // It means it's the "6 for £6" Guardian Weekly offer,
        // which starts with a six week one-off billing period then
        // changes to quarterly recurring at the end of the six weeks.
        case SixWeeks => true
        case _ => false
      }

      val paymentFrequency = plan.charges.billingPeriod match {
        case oneOff: OneOffPeriod => oneOff match {
          case _ if isSixForSix => PaymentFrequency.Quarterly

          // Any other one-off billing periods are probably impossible for new acquisitions,
          // and only exist to model legacy print subscriptions.
          // So we're fine to store these as OneOff.
          case _ => PaymentFrequency.OneOff
        }

        case recurring: RecurringPeriod => recurring match {
          case Month => PaymentFrequency.Monthly
          case Quarter => PaymentFrequency.Quarterly
          case SixMonthsRecurring => PaymentFrequency.SixMonthly
          case Year => PaymentFrequency.Annually
        }
      }

      Right(
        Acquisition(
          product,
          paymentFrequency,
          currency = currency.iso,

          amount = plan.charges.price.getPrice(currency)
            .map(_.amount.toDouble).getOrElse(0),

          paymentProvider = paymentData match {
            case DirectDebitData(_, _, _) => Some(Gocardless)
            case CreditCardData(_) => Some(Stripe)
            case _ => {
              logger.error("No payment provider for acquisition event")
              None
            }
          },

          countryCode = personalData.address.country.map(_.alpha2),

          printOptions = productData match {
            case Left(paperData) => printOptionsFromPaperData(paperData)
            case _ => None
          },

          discountPercentage = promotion.map(_.promotionType) collect {
            case PercentDiscount(_, amount) => amount
          },

          discountLengthInMonths = promotion.map(_.promotionType) collect {
            case PercentDiscount(Some(months), _) => months.toShort
          },

          campaignCode = referrerAcquisitionData.flatMap(_.campaignCode.map(Set(_))),
          abTests = getAbTests(referrerAcquisitionData),
          referrerPageViewId = referrerAcquisitionData.flatMap(_.referrerPageviewId),
          referrerUrl = referrerAcquisitionData.flatMap(_.referrerUrl),
          componentId = referrerAcquisitionData.flatMap(_.componentId),
          componentTypeV2 = referrerAcquisitionData.flatMap(_.componentType),
          source = referrerAcquisitionData.flatMap(_.source),
          platform = Some(Platform.Membership),
          promoCode = promoCode.map(_.get),
          labels = if (isSixForSix) Some(Set("guardian-weekly-six-for-six")) else None
        )
      )
    }
  }
}
