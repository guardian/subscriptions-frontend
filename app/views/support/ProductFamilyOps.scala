package views.support

import com.gu.memsub._

object ProductFamilyOps {

  implicit class ProductFamilyName(in: ProductFamily) {

    def title(plan: PaidPlan[Current, BillingPeriod]): String = in match {
      case Digipack => "Guardian Digital Pack"
      case Paper => plan.description
    }

    def subtitle(plan: PaidPlan[Current, BillingPeriod]): String = in match {
      case Digipack => "Daily Edition + Guardian App Premium Tier"
      case Paper => plan.name + " package"
    }

    def packImage: String = in match {
      case Digipack => "images/digital-pack.png"
      case Paper => "images/backgrounds/bg-paper-only.png"
    }

    def changeRatePlanText: String = in match {
      case Digipack => "Change payment frequency"
      case Paper => "Change package"
    }

    def email: String = in match {
      case Digipack => "digitalpack@theguardian.com"
      case Paper => "todo@theguardian.com" // TODO
    }

    def phone: String = "+44 (0) 330 333 6767"

  }

}
