package views.support

import com.gu.i18n.Country
import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod.SixWeeks
import com.gu.memsub.Product.{Delivery, Voucher}
import com.gu.memsub._
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.subsv2.CatalogPlan.ContentSubscription
import com.gu.memsub.subsv2.{CatalogPlan, Plan}
import io.lemonlabs.uri.typesafe.dsl._

import scala.reflect.internal.util.StringOps
object PlanOps {
  implicit class CommonPlanOps[+A <: Plan[_,_]](in: A) {

    def title: String = in.product match {
      case Product.Digipack => "Guardian Digital Pack"
      case _: Product.Weekly  => "Guardian Weekly"
      case _ => "Guardian/Observer Newspapers"
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

    def commissionGroup: String = {
      in.product match {
        case Product.Digipack => "Digital"
        case _: Product.Weekly=> "Weekly"
        case Product.Voucher | Product.Delivery => "Newspaper"
        case _ => ""
      }
    }
  }

  implicit class PrettyPlan[+A <: CatalogPlan.Paid](in: A) {

    def packImage: ResponsiveImageGroup = in.charges.benefits.list.toList match {
      case Digipack :: Nil =>
        ResponsiveImageGroup(
          availableImages = Seq(ResponsiveImage(controllers.CachedAssets.hashedPathFor("images/digital-pack-garnett.png"), 300)),
            altText = Some("Guardian apps demoed on Apple and Android devices")
        )
      case Weekly :: Nil =>
        ResponsiveImageGroup(
          availableImages = ResponsiveImageGenerator("cfcdcca420678e73988d0c89cd214afca40b7c2f/0_0_673_880", Seq(382), "png"),
          altText = Some("A Guardian Weekly cover")
        )
      case _ =>
        ResponsiveImageGroup(
          availableImages = ResponsiveImageGenerator("c3dbecb41fd1538d10379ec8fb557d54c1bdac10/0_0_459_500", Seq(459)),
          altText = Some("Stack of The Guardian newspapers")
        )
    }

    def changeRatePlanText: String = in.charges.benefits.list.toList match {
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

    def hasPhysicalBenefits: Boolean = in.charges.benefits.list.toList.exists(_.isPhysical)

    def hasDigitalPack: Boolean = in.charges.benefits.list.toList.contains(Digipack)

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

    def email(contactUsCountry: Option[Country]): String =
      product match {
        case Delivery => "homedelivery@theguardian.com"
        case Voucher => "vouchersubs@theguardian.com"
        case Product.Digipack => ContactCentreOps.digiPackEmail(contactUsCountry)
        case _: Product.Weekly => ContactCentreOps.weeklyEmail(contactUsCountry)
        case _ => ContactCentreOps.email
      }

    def faqHref: String =
      product match {
        case _: Product.Weekly => "https://www.theguardian.com/help/2012/jan/19/guardian-weekly-faqs"
        case _ => "https://www.theguardian.com/subscriber-direct/subscription-frequently-asked-questions"
      }

    def phone(contactUsCountry: Option[Country]): String = ContactCentreOps.phone(contactUsCountry)

    def productType: String = product match {
      case Delivery => "Home Delivery"
      case Voucher => "Voucher Book"
      case Product.Digipack => "Digital Pack"
      case _: Product.Weekly => "Guardian Weekly"
      case _ => "Unknown"
    }

  }

  implicit class OptionProductOps(maybeProduct: Option[Product]) {
    def email(contactUsCountry: Option[Country]): String = maybeProduct.map(_.email(contactUsCountry)).getOrElse(ContactCentreOps.email)
    def phone(contactUsCountry: Option[Country]): String = maybeProduct.map(_.phone(contactUsCountry)).getOrElse(ContactCentreOps.phone(contactUsCountry))
  }

  implicit class ProductPopulationDataOps(in: ProductPopulationData) {
    val products: ContentSubscription = in.plans.default
    def isHomeDelivery: Boolean = products.isHomeDelivery
    def isGuardianWeekly: Boolean = products.isGuardianWeekly
    def isPhysical: Boolean = products.hasPhysicalBenefits
    def isVoucher: Boolean = products.isVoucher
    def isDigitalPack: Boolean = products.isDigitalPack
    def isSixForSix: Boolean = products.promotionalOnly
  }

}
