package views.support

import com.gu.i18n.Currency
import com.gu.i18n.Currency.{CAD, GBP}
import com.gu.memsub.Benefit.{Digipack, Weekly}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.promo.PercentDiscount.getDiscountScaledToPeriod
import com.gu.memsub.promo.{LandingPage, PercentDiscount, Promotion}
import com.gu.memsub.subsv2._
import views.support.BillingPeriod._
import views.support.PlanOps._

import scala.language.higherKinds

object Pricing {

  implicit class PlanWithPricing[+A <: PaidChargeList](plan: A) {
    lazy val gbpPrice = plan.price.getPrice(GBP).get

    def unsafePrice(currency: Currency) = plan.price.getPrice(currency).getOrElse(
      throw new NoSuchElementException(s"Could not find a price in $currency for charge list")
    )
    def prettyPricing(currency: Currency) =
      s"${unsafePrice(currency).pretty} ${plan.billingPeriod.frequencyInMonths}"

    def amount(currency: Currency) = unsafePrice(currency).amount

    def prettyPricingForDiscountedPeriod[M[+A], A <: LandingPage](discountPromo: Promotion[PercentDiscount, M, A], currency: Currency) = {
      val originalAmount = unsafePrice(currency)
      val discountAmount = discountPromo.promotionType.applyDiscount(originalAmount, plan.billingPeriod)

      discountPromo.promotionType.durationMonths.fold {
        s"${discountAmount.pretty} ${plan.billingPeriod.frequencyInMonths}"
      } { durationMonths =>

        val forDuration = plan.billingPeriod match {
          case Month =>
            val span = durationMonths
            if (span == 12) {
              "every month for 1 year"
            } else if (span > 1) {
              s"every month for $span months"
            } else {
              "for 1 month"
            }
          case Quarter =>
            val span = getDiscountScaledToPeriod(discountPromo.promotionType, Quarter)._2
            if (span == 4) {
              "every quarter for 1 year"
            } else if (span > 1) {
              s"every quarter for ${span.toInt} quarters"
            } else {
              "for 1 quarter"
            }
          case Year =>
            val span = getDiscountScaledToPeriod(discountPromo.promotionType, Year)._2
            if (span > 1) {
              s"every year for ${span.toInt} years"
            } else {
              "for 1 year"
            }
          case OneYear => s"for 1 year only"
          case _ => ""
        }
        if (plan.billingPeriod == OneYear) { s"${discountAmount.pretty} $forDuration" } else {
          s"${discountAmount.pretty} $forDuration, then standard rate (${originalAmount.pretty} every ${plan.billingPeriod.noun})"
        }
      }
    }

    def headlinePricingForDiscountedPeriod[M[+A], A <: LandingPage](discountPromo: Promotion[PercentDiscount, M, A], currency: Currency) = {
      val originalAmount = unsafePrice(currency)
      val discountAmount = discountPromo.promotionType.applyDiscount(originalAmount, plan.billingPeriod)
      val from = discountPromo.promotionType.durationMonths.map(_ => "From")
      s"${from.mkString} ${discountAmount.pretty} per ${plan.billingPeriod.noun}"
    }
  }

  implicit class PrettyProductPlan(in: CatalogPlan.Paid) {
    implicit val planWithPricing = new PlanWithPricing(in.charges)

    def prefix = in.charges.benefits.list.toList match {
      case Digipack :: Nil => ""
      case b => if (b.contains(Weekly)) "" else s"${in.packageName} - "
    }

    def prettyName(currency: Currency): String = in.charges.benefits.list.toList match {
      case Digipack :: Nil | Weekly :: Nil => planWithPricing.prettyPricing(currency)
      case _ => s"$prefix${planWithPricing.prettyPricing(currency)}"
    }
  }

}
