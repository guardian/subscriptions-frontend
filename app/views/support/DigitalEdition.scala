package views.support
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import controllers.routes
import model.DigitalEdition._
import model.{DigitalEdition => DE}
import utils.Tracking.internalCampaignCode

object DigitalEdition {

  implicit class DigitalEditionOps(edition: DE) {

    lazy val DEFAULT_CAMPAIGN_CODE = s"GU_SUBSCRIPTIONS_${edition.id.toUpperCase}_PROMO"

    def redirect: Uri = {
      "/checkout" ? ("countryGroup" -> edition.countryGroup.id)
    }

    def membershipLandingPage = getMembershipLandingPage(DEFAULT_CAMPAIGN_CODE)

    def getMembershipLandingPage(campaignCode: String): Uri = {
      val params = internalCampaignCode -> campaignCode
      edition match {
        case AU => "https://membership.theguardian.com/au/supporter" ? params
        case INT => "https://membership.theguardian.com/int/supporter" ? params
        case US => "https://membership.theguardian.com/us/supporter" ? params
        case _ => "https://membership.theguardian.com/join" ? params
      }
    }

    def getDestinationUrl(promoCode: String, includeEdition: Boolean) = if(includeEdition)
      routes.PromoLandingPage.render(promoCode, None).url + "?edition=" + edition.id
    else
      routes.PromoLandingPage.render(promoCode, edition.countryGroup.defaultCountry).url
  }

}
