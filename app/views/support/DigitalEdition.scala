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

    def redirect(referringButton: String): Uri = {
      "/checkout" ? ("countryGroup" -> edition.countryGroup.id) & ("startTrialButton" -> referringButton)
    }

    def getDestinationUrl(promoCode: String, includeEdition: Boolean) = if(includeEdition)
      routes.PromoLandingPage.render(promoCode, None).url + "?edition=" + edition.id
    else
      routes.PromoLandingPage.render(promoCode, edition.countryGroup.defaultCountry).url
  }

}
