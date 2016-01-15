package views.support

import com.gu.i18n.{Currency, GBP}
import com.gu.memsub.{BillingPeriod => BP}
import com.gu.subscriptions.DigipackPlan
import views.support.BillingPeriod._

object Pricing {
  implicit class PlanWithPricing(digipackPlan: DigipackPlan[BP]) {
    lazy val gbpPrice = digipackPlan.pricing.getPrice(GBP).get

    private def unsafePrice(currency: Currency) = digipackPlan.pricing.getPrice(currency).getOrElse(
      throw new NoSuchElementException(s"Could not find a price in $currency for plan ${digipackPlan.name}")
    )


    def prettyPricing(currency: Currency) =
      s"${unsafePrice(currency).pretty} ${digipackPlan.billingPeriod.frequencyInMonths}"

    def amount(currency: Currency) =
      unsafePrice(currency).amount
  }
}
