package views.support
import com.gu.memsub.Product.{Delivery, Voucher}

import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.subsv2.CatalogPlan
import com.netaporter.uri.dsl._

import scalaz.syntax.std.boolean._
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
      in.isHomeDelivery.option("homedelivery@theguardian.com") orElse
      in.isVoucher.option("vouchersubs@theguardian.com") orElse
      in.hasDigitalPack.option("digitalpack@theguardian.com")
    ).getOrElse("subscriptions@theguardian.com")

    def isHomeDelivery: Boolean = in.product == Delivery

    def isVoucher: Boolean = in.product == Voucher

    def isDigitalPack: Boolean = in.product == com.gu.memsub.Product.Digipack

    def hasDigitalPack: Boolean = in.charges.benefits.list.contains(Digipack)

    def phone: String = "0330 333 6767"

    def productType: String = {
      if (isHomeDelivery) {
        "Home Delivery"
      } else if (isVoucher) {
        "Voucher Book"
      } else if (isDigitalPack) {
        "Digital Pack"
      } else {
        "Unknown"
      }
    }
  }

  implicit class ProductPopulationDataOps(in: ProductPopulationData) {
    val products = in.plans.list.head
    def isHomeDelivery: Boolean = products.isHomeDelivery
    def isPhysical: Boolean = products.isHomeDelivery
    def isVoucher: Boolean = products.isVoucher
    def isDigitalPack: Boolean = products.isDigitalPack
    def productType: String = products.productType
  }

}
