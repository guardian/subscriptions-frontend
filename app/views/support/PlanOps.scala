package views.support
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.subsv2.{CatalogPlan, Delivery, Voucher}
import scalaz.syntax.std.boolean._
import com.netaporter.uri.dsl._
object PlanOps {

  implicit class PrettyPlan[+A <: CatalogPlan.AnyPlan](in: A) {

    def title: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Guardian Digital Pack"
      case _ => s"${in.name} package"
    }

    def subtitle: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Daily Edition + Guardian App Premium Tier"
      case _ => in.description
    }

    def packImage: ResponsiveImageGroup = in.charges.benefits.list match {
      case Digipack :: Nil => ResponsiveImageGroup(availableImages = Seq(ResponsiveImage(controllers.CachedAssets.hashedPathFor("images/digital-pack.png"), 300)))
      case _ => ResponsiveImageGroup(availableImages = ResponsiveImageGenerator("05129395fe0461071f176f526d7a4ae2b1d9b9bf/0_0_5863_5116", Seq(140, 500, 1000, 2000)))
    }

    def changeRatePlanText: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Change payment frequency"
      case _ => "Add more"
    }

    def email: String = (
      in.hasHomeDelivery.option("homedelivery@theguardian.com") orElse
      in.hasVoucher.option("vouchersubs@theguardian.com") orElse
      in.hasDigitalPack.option("digitalpack@theguardian.com")
    ).getOrElse("subscriptions@theguardian.com")

    def hasHomeDelivery: Boolean = in.product == Delivery

    def hasVoucher: Boolean = in.product == Voucher

    def hasDigitalPack: Boolean = in.charges.benefits.list.contains(Digipack)

    def phone: String = "0330 333 6767"
  }

  implicit class ProductPopulationDataOps(in: ProductPopulationData) {
    val products = in.plans.list.head
    def hasHomeDelivery: Boolean = products.product == com.gu.memsub.subsv2.Delivery
    def hasVoucher: Boolean = products.product == com.gu.memsub.subsv2.Voucher

  }

}
