package model

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

case class DigitalEdition(countryGroup: CountryGroup, id: String, name: String, campaign: String)


object DigitalEdition {
  object UK extends DigitalEdition(CountryGroup.UK, "uk", "UK", "dis_2408")
  object US extends DigitalEdition(CountryGroup.US, "us", "US", "dis_2378")
  object AU extends DigitalEdition(CountryGroup.Australia, "au", "Australia", "dis_2379")

  def getRedirect(edition: DigitalEdition): Uri = edition match {
      case UK => "/digital/country"
      case _ => "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=dga38&CMP=FAB_3062"
  }

  def getMembershipLandingPage(edition: DigitalEdition): Uri = {
    val params = "INTCMP" -> s"GU_SUBSCRIPTIONS_${edition.id.toUpperCase}_PROMO"

    edition match {
      case US => "https://membership.theguardian.com/us/supporter" ? params
      case _ => "https://membership.theguardian.com/join" ? params
    }
  }
}
