package views.support

import com.gu.memsub.promo.Promotion.{AnyPromotion, AnyPromotionWithLandingPage}
import com.gu.memsub.promo._

object LandingPageOps {

  private def getSectionColour(promotion: AnyPromotionWithLandingPage) = promotion.landingPage.sectionColour.getOrElse(Blue)

  implicit class ToLandingPageClassName(promotion: AnyPromotionWithLandingPage) {
    def landingPageSectionColour: String = {
      getSectionColour(promotion) match {
        case Blue => "section-blue"
        case Grey => "section-grey"
        case White => "section-white"
      }
    }
  }

  implicit class GetSectionSeparator(promotion: AnyPromotionWithLandingPage) {
    def getSectionSeparator: String = {
      getSectionColour(promotion) match {
        case White => "section-separator"
        case _ => if (promotion.whenFreeTrial.isDefined) "section-separator" else ""
      }
    }
  }

  implicit class GetIncentiveTermsAndConditions(promotion: AnyPromotion) {
    def getIncentiveTermsAndConditions: String = {
      promotion.whenIncentive.fold("") { p => p.promotionType.termsAndConditions.mkString }
    }
  }

}
