package model

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

case class DigitalEdition(price: String, campaign: String, countryGroup: CountryGroup) {
  def id = countryGroup.id
  def name = countryGroup.name
}

object DigitalEdition {

  object UK extends DigitalEdition("£11.99", "dis_2408", CountryGroup.UK)

  object US extends DigitalEdition("$19.99", "dis_2378", CountryGroup.US)

  object AU extends DigitalEdition("$21.50", "dis_2379", CountryGroup.Australia)

  def getRedirect(edition: DigitalEdition): Uri = {
    "/checkout" ? ("countryGroup" -> edition.countryGroup.id)
  }

  def getMembershipLandingPage(edition: DigitalEdition): Uri = {
    val params = "INTCMP" -> s"GU_SUBSCRIPTIONS_${edition.id.toUpperCase}_PROMO"

    edition match {
      case US => "https://membership.theguardian.com/us/supporter" ? params
      case _ => "https://membership.theguardian.com/join" ? params
    }
  }
}
