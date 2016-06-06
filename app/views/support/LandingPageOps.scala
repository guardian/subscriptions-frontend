package views.support

import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.memsub.promo._
import org.joda.time.DateTime.now
import scalaz.Id._

object LandingPageOps {

  private def getSectionColour(landingPage: SubscriptionsLandingPage) = landingPage.sectionColour.getOrElse(Blue)

  implicit class ForPromotionsWithALandingPage(promotion: Promotion[PromotionType, Id, SubscriptionsLandingPage]) {
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
      promotion.asIncentive.fold("") { p => p.promotionType.termsAndConditions.mkString }
    }
  }

}
