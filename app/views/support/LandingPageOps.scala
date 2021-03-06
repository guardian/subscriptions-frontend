package views.support

import com.gu.i18n.CountryGroup.UK
import com.gu.i18n.Currency.GBP
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.subsv2.CatalogPlan
import controllers.routes
import model.Subscriptions.SubscriptionOption
import org.joda.time.DateTime.now
import views.support.Catalog._

object LandingPageOps {

  private def getSectionColour(landingPage: DigitalPackLandingPage) = landingPage.sectionColour.getOrElse(Blue)

  implicit class ForPromoWithDigitalPackLandingPage(promotion: PromoWithDigitalPackLandingPage) {
    def landingPageSectionColour: String = {
      getSectionColour(promotion.landingPage) match {
        case Blue => "section-blue"
        case Grey => "section-grey"
        case White => "section-white"
      }
    }
    def getSectionSeparator: String = {
      getSectionColour(promotion.landingPage) match {
        case White => "section-separator"
        case _ => if (promotion.asFreeTrial.isDefined) "section-separator" else ""
      }
    }
    def getDescriptionBorder: String = {
      if (promotion.landingPage.image.isDefined && promotion.expires.exists(now.plusMonths(1).isAfter))
        "promotion-description--bordered"
      else
        ""
    }
  }

  implicit class ForAnyPromotions(promotion: AnyPromotion) {
    def getIncentiveTermsAndConditions: String = {
      promotion.asIncentive.flatMap(_.promotionType.termsAndConditions).mkString
    }
    def getIncentiveLegalTerms: String = {
      promotion.asIncentive.flatMap(_.promotionType.legalTerms).mkString
    }
  }

  def planToOptions(promoCode: PromoCode, promotion: AnyPromotion)(in: CatalogPlan.Paid): SubscriptionOption = {
    val planPrice = promotion.asDiscount.map(adjustPrice(in, GBP, _)).getOrElse(in.charges.gbpPrice)
    val saving = if (promotion.asDiscount.isDefined) None else in.saving
    val paymentDetails = promotion.asDiscount.map(views.html.fragments.promotion.paymentDetails(in, GBP, _))

    SubscriptionOption(in.id.get,
      in.name,
      planPrice.amount * 12 / 52,
      saving.map(_.toString + "%"),
      planPrice.amount,
      in.description,
      routes.Checkout.renderCheckout(UK.id, Some(promoCode), None, in.slug).url,
      paymentDetails
    )
  }
}
