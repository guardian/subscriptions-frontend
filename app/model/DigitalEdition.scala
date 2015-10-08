package model

import configuration.Config

case class DigitalEdition(id: String, name: String, price: String, campaign: String)


object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

  def getRedirect(edition: DigitalEdition, externalSubscriptionUrl: String): String = {
    edition match {
      case UK => "/digital/country"
      case DigitalEdition(_, _, _, campaign) => externalSubscriptionUrl + "?prom=DGA38&CMP=" + campaign
    }
  }
}
