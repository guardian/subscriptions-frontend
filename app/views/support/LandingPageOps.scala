package views.support

import com.gu.memsub.promo.Promotion.{AnyPromotion, AnyPromotionWithLandingPage}
import com.gu.memsub.promo._
import org.joda.time.DateTime.now

object LandingPageOps {

  private def getSectionColour(promotion: AnyPromotionWithLandingPage) = promotion.landingPage.sectionColour.getOrElse(Blue)

  implicit class ForPromotionsWithALandingPage(promotion: AnyPromotionWithLandingPage) {
    def landingPageSectionColour: String = {
      getSectionColour(promotion) match {
        case Blue => "section-blue"
        case Grey => "section-grey"
        case White => "section-white"
      }
    }
    def getSectionSeparator: String = {
      getSectionColour(promotion) match {
        case White => "section-separator"
        case _ => if (promotion.whenFreeTrial.isDefined) "section-separator" else ""
      }
    }
    def getDescriptionBorder: String = {
      if (promotion.landingPage.imageUrl.isDefined && promotion.expires.exists(now.plusMonths(1).isAfter))
        "promotion-description--bordered"
      else
        ""
    }
  }

  implicit class ForAnyPromotions(promotion: AnyPromotion) {
    def getIncentiveTermsAndConditions: String = {
      promotion.whenIncentive.fold("") { p => p.promotionType.termsAndConditions.mkString }
    }
  }

}
