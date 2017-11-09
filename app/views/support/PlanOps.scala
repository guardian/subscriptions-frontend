package views.support

import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod.SixWeeks
import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.subsv2.{CatalogPlan, Plan}
import com.netaporter.uri.dsl._
import model.DigitalEdition.{AU, UK, US}

import scala.reflect.internal.util.StringOps
object PlanOps {
  implicit class CommonPlanOps[+A <: Plan[_,_]](in: A) {

    def title: String = in.product match {
      case Product.Digipack => "Guardian Digital Pack"
      case _: Product.Weekly  => "Guardian Weekly"
      case x => "Guardian/Observer Newspapers"
    }

    def packageName: String = in.product match {
      case Product.Digipack => "Guardian Digital Pack"
      case _: Product.Weekly=> "The Guardian Weekly"
      case _ => s"${in.name} package"
    }

    def subtitle: Option[String] = in.product match {
      case Product.Digipack => Some("Daily Edition + Guardian App Premium Tier")
      case _ => StringOps.oempty(in.description).headOption
    }

    def segment: String = {
      in.product match {
        case Product.Digipack => "digital"
        case _: Product.Weekly=> "weekly"
        case Product.Voucher | Product.Delivery => if (in.name.endsWith("+")) "paper-digital" else "paper"
        case _ => ""
      }
    }
  }

  implicit class PrettyPlan[+A <: CatalogPlan.Paid](in: A) {

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

    def promotionalOnly: Boolean = in.charges.billingPeriod == SixWeeks

    def isHomeDelivery: Boolean = in.product == Delivery

    def isVoucher: Boolean = in.product == Voucher

    def isDigitalPack: Boolean = in.product == com.gu.memsub.Product.Digipack

    def isGuardianWeekly: Boolean = in.product match {
      case _:Product.Weekly => true
      case _ => false
    }

    def hasPhysicalBenefits: Boolean = in.charges.benefits.list.exists(_.isPhysical)

    def hasDigitalPack: Boolean = in.charges.benefits.list.contains(Digipack)

    def segment: String = {
      if (in.isDigitalPack) {
        "digital"
      } else if (in.isHomeDelivery || in.isVoucher) {
        if (in.hasDigitalPack) {
          "paper-digital"
        } else {
          "paper"
        }
      } else if (in.isGuardianWeekly) {
        "weekly"
      } else {
        ""
      }
    }
  }

  implicit class ProductOps(product: Product) {

    def email: String =
      product match {
        case Delivery => "homedelivery@theguardian.com"
        case Voucher => "vouchersubs@theguardian.com"
        case Product.Digipack => "digitalpack@theguardian.com"
        case _: Product.Weekly => "gwsubs@theguardian.com"
        case _ => "subscriptions@theguardian.com"
      }

    def faqHref: String =
      product match {
        case _: Product.Weekly => "https://www.theguardian.com/help/2012/jan/19/guardian-weekly-faqs"
        case _ => "https://www.theguardian.com/subscriber-direct/subscription-frequently-asked-questions"
      }

    def phone(digitalEdition: model.DigitalEdition): String = product match {
      case _: Product.Weekly if digitalEdition == US => {"1-844-632-2010 (toll free); 917-900-4663 (direct line)"}
      case _: Product.Weekly if digitalEdition == AU => {"1800 773 766 (toll free within Australia)"}
      case _ => {"+44 (0) 330 333 6767. Open BST 8am to 8pm, Monday to Sunday"}
    }

    def productType: String = product match {
      case Delivery => "Home Delivery"
      case Voucher => "Voucher Book"
      case Product.Digipack => "Digital Pack"
      case _: Product.Weekly => "Guardian Weekly"
      case _ => "Unknown"
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
