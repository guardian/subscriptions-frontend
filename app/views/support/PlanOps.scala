package views.support

import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.subsv2.CatalogPlan
import com.netaporter.uri.dsl._

import scala.reflect.internal.util.StringOps
import scalaz.syntax.std.boolean._
object PlanOps {

  implicit class PrettyPlan[+A <: CatalogPlan.AnyPlan](in: A) {

    def title: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Guardian Digital Pack"
      case Weekly :: Nil => "Guardian Weekly"
      case x => "Guardian/Observer Newspapers"
    }

    def packageName: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Guardian Digital Pack"
      case Weekly :: Nil => "The Guardian Weekly"
      case _ => s"${in.name} package"
    }

    def subtitle: Option[String] = in.charges.benefits.list match {
      case Digipack :: Nil => Some("Daily Edition + Guardian App Premium Tier")
      case _ => StringOps.oempty(in.description).headOption
    }

    def packImage: ResponsiveImageGroup = in.charges.benefits.list match {
      case Digipack :: Nil =>
        ResponsiveImageGroup(
          availableImages = Seq(ResponsiveImage(controllers.CachedAssets.hashedPathFor("images/digital-pack.png"), 300)),
            altText = Some("Guardian apps demoed on Apple, Android and Kindle Fire devices")
        )
      case Weekly :: Nil =>
        ResponsiveImageGroup(
          availableImages = ResponsiveImageGenerator("f3f9792bbb2c2f04ef01d0c1022e04eb87a2619a/0_0_4350_3416", Seq(500, 1000)),
          altText = Some("Stack of The Guardian Weekly editions")
        )
      case _ =>
        ResponsiveImageGroup(
          availableImages = ResponsiveImageGenerator("05129395fe0461071f176f526d7a4ae2b1d9b9bf/0_0_5863_5116", Seq(140, 500, 1000, 2000)),
          altText = Some("Stack of The Guardian newspapers")
        )
    }

    def changeRatePlanText: String = in.charges.benefits.list match {
      case Digipack :: Nil | Weekly :: Nil=> "Change payment frequency"
      case _ => "Add more"
    }

    def email: String = (
      in.isHomeDelivery.option("homedelivery@theguardian.com") orElse
      in.isVoucher.option("vouchersubs@theguardian.com") orElse
      in.hasDigitalPack.option("digitalpack@theguardian.com") orElse
      in.isGuardianWeekly.option("gwsubs@theguardian.com")
    ).getOrElse("subscriptions@theguardian.com")

    def isHomeDelivery: Boolean = in.product == Delivery

    def isVoucher: Boolean = in.product == Voucher

    def isDigitalPack: Boolean = in.product == com.gu.memsub.Product.Digipack

    def isGuardianWeekly: Boolean = in.product match {
      case _:Product.Weekly => true
      case _ => false
    }

    def hasPhysicalBenefits: Boolean = in.charges.benefits.list.exists(_.isPhysical)

    def hasDigitalPack: Boolean = in.charges.benefits.list.contains(Digipack)

    def phone: String = "+44 (0) 330 333 6767"

    def productType: String = {
      if (isHomeDelivery) {
        "Home Delivery"
      } else if (isVoucher) {
        "Voucher Book"
      } else if (isDigitalPack) {
        "Digital Pack"
      } else if (isGuardianWeekly) {
        "Guardian Weekly"
      } else {
        "Unknown"
      }
    }
  }

  implicit class ProductPopulationDataOps(in: ProductPopulationData) {
    val products = in.plans.default
    def isHomeDelivery: Boolean = products.isHomeDelivery
    def isGuardianWeekly: Boolean = products.isGuardianWeekly
    def isPhysical: Boolean = products.hasPhysicalBenefits
    def isVoucher: Boolean = products.isVoucher
    def isDigitalPack: Boolean = products.isDigitalPack
  }

}
