package views.support

import com.gu.i18n.GBP
import com.gu.memsub.{BillingPeriod => BP}
import com.gu.subscriptions.DigipackPlan


object Pricing {

  implicit class PlanWithPricing(digipackPlan: DigipackPlan[BP]) {
    def gbpPrice = digipackPlan.pricing.getPrice(GBP).get
  }
}
