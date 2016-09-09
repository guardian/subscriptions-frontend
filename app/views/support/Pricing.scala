package views.support

import com.gu.i18n.{Currency, GBP}
import com.gu.memsub.promo.PercentDiscount.getDiscountScaledToPeriod
import com.gu.memsub.promo.{LandingPage, PercentDiscount, Promotion}
import com.gu.memsub.{BillingPeriod => BP, _}
import com.gu.memsub.subsv2._
import views.support.BillingPeriod._
import views.support.PlanOps._

object Pricing {

  implicit class PlanWithPricing[+A <: PaidChargeList](plan: A) {
    lazy val gbpPrice = plan.price.getPrice(GBP).get

    def unsafePrice(currency: Currency) = plan.price.getPrice(currency).getOrElse(
      throw new NoSuchElementException(s"Could not find a price in $currency for charge list")
    )

    def prettyPricing(currency: Currency) =
      s"${unsafePrice(currency).pretty} ${plan.billingPeriod.frequencyInMonths}"

    def amount(currency: Currency) =
      unsafePrice(currency).amount

    def prettyPricingForDiscountedPeriod(discountPromo: Promotion[PercentDiscount, Option, LandingPage], currency: Currency) = {
      val originalAmount = unsafePrice(currency)
      val discountAmount = discountPromo.promotionType.applyDiscount(originalAmount, plan.billingPeriod)

      discountPromo.promotionType.durationMonths.fold {
        s"${discountAmount.pretty} ${plan.billingPeriod.frequencyInMonths}"
      } { durationMonths =>
        plan.billingPeriod match {
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
  implicit class PrettyProductPlan(in: CatalogPlan.Paid) {
    implicit val planWithPricing = new PlanWithPricing(in.charges)

    def prefix = in.charges.benefits.list match {
      case Digipack :: Nil => ""
      case _ => s"${in.title} - "
    }

    def prettyName(currency: Currency): String = in.charges.benefits.list match {
      case Digipack :: Nil => planWithPricing.prettyPricing(currency)
      case _ => s"$prefix${planWithPricing.prettyPricing(currency)}"
    }
  }
}
