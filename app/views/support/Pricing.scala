package views.support

import com.gu.i18n.{Currency, GBP}
import com.gu.memsub.promo.PercentDiscount.getDiscountScaledToPeriod
import com.gu.memsub.promo.{LandingPage, PercentDiscount, Promotion}
import com.gu.memsub.{Month, Quarter, Year, BillingPeriod => BP}
import com.gu.subscriptions.DigipackPlan
import views.support.BillingPeriod._

object Pricing {
  implicit class PlanWithPricing(digipackPlan: DigipackPlan[BP]) {
    lazy val gbpPrice = digipackPlan.pricing.getPrice(GBP).get

    def unsafePrice(currency: Currency) = digipackPlan.pricing.getPrice(currency).getOrElse(
      throw new NoSuchElementException(s"Could not find a price in $currency for plan ${digipackPlan.name}")
    )

    def prettyPricing(currency: Currency) =
      s"${unsafePrice(currency).pretty} ${digipackPlan.billingPeriod.frequencyInMonths}"

    def amount(currency: Currency) =
      unsafePrice(currency).amount

    def prettyPricingForDiscountedPeriod(discountPromo: Promotion[PercentDiscount, Option, LandingPage], currency: Currency) = {
      val originalAmount = unsafePrice(currency)
      val discountAmount = discountPromo.promotionType.applyDiscount(originalAmount, digipackPlan.billingPeriod)

      discountPromo.promotionType.durationMonths.fold {
        s"${discountAmount.pretty} ${digipackPlan.billingPeriod.frequencyInMonths}"
      } { durationMonths =>
        digipackPlan.billingPeriod match {
          case m: Month =>
            val span = durationMonths
            if (span > 1) {
              s"${discountAmount.pretty} for $span months, then ${originalAmount.pretty} every month thereafter"
            } else {
              s"${discountAmount.pretty} for 1 month, then ${originalAmount.pretty} every month thereafter"
            }
          case q: Quarter =>
            val span = getDiscountScaledToPeriod(discountPromo.promotionType, q)._2
            if (span > 1) {
              s"${discountAmount.pretty} for ${span.toInt} quarters, then ${originalAmount.pretty} every quarter thereafter"
            } else {
              s"${discountAmount.pretty} for 1 quarter, then ${originalAmount.pretty} every quarter thereafter"
            }
          case y: Year =>
            val span = getDiscountScaledToPeriod(discountPromo.promotionType, y)._2
            if (span > 1) {
              s"${discountAmount.pretty} for ${span.toInt} years, then ${originalAmount.pretty} every year thereafter"
            } else {
              s"${discountAmount.pretty} for 1 year, then ${originalAmount.pretty} every year thereafter"
            }
        }
      }
    }
  }
}
