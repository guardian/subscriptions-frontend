package views.support
import com.gu.memsub._
import com.gu.subscriptions.{ProductList, ProductPlan}

object PlanOps {

  implicit class ToProductPlanOps(in: ProductPlan[ProductList]) {

    def title: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "Guardian Digital Pack"
      case _ => in.name + " package"
    }

    def subtitle: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "Daily Edition + Guardian App Premium Tier"
      case _ => in.description
    }

    def packImage: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "images/digital-pack.png"
      case _ => "images/backgrounds/bg-paper-only.png"
    }

    def changeRatePlanText: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "Change payment frequency"
      case _ => "Add more"
    }

    def email: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "digitalpack@theguardian.com"
      case _ => "todo@theguardian.com" // TODO
    }

    def phone: String = "+44 (0) 330 333 6767"
  }
}
