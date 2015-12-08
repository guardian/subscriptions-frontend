package model

case class DigitalEdition(id: String, name: String, price: String, campaign: String)


object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

  def getRedirect(edition: DigitalEdition): String = edition match {
      case UK => "/digital/country"
      case _ => "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=dga38&CMP=FAB_3062"
  }

  def getMembershipLandingPage(edition: DigitalEdition): String =  edition match {
    case US => "https://membership.theguardian.com/us/supporter"
    case _ => "https://membership.theguardian.com/join"
  }
}
