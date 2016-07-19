package views.support
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.subscriptions.{ProductList, ProductPlan}
import com.netaporter.uri.dsl._
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

    def packImage: ResponsiveImageGroup = in match {
      case _ if in.products.seq == Seq(Digipack) => ResponsiveImageGroup(availableImages = Seq(ResponsiveImage(controllers.CachedAssets.hashedPathFor("images/digital-pack.png"), 300)))
      case _ => ResponsiveImageGroup(availableImages = ResponsiveImageGenerator("05129395fe0461071f176f526d7a4ae2b1d9b9bf/0_0_5863_5116", Seq(140, 500, 1000, 2000)))
    }

    def changeRatePlanText: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "Change payment frequency"
      case _ => "Add more"
    }

    def email: String = in match {
      case _ if in.products.seq == Seq(Digipack) => "digitalpack@theguardian.com"
      case _ if in.products.seq.contains(Delivery) => "homedelivery@theguardian.com"
      case _ if !in.products.seq.contains(Delivery) => "vouchersubs@theguardian.com"
      case _ => "subscriptions@theguardian.com"
    }

    def phone: String = "+44 (0) 330 333 6767"
  }
}
