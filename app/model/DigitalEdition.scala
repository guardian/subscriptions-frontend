package model

import configuration.Config

case class DigitalEdition(id: String, name: String, price: String, campaign: String)


object DigitalEdition {

  trait UrlConfig {
    val externalSubscriptionUrl: String
  }

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")
  implicit lazy val defaultUrlConfig: UrlConfig = Config

  def getRedirect(edition: DigitalEdition)(implicit urlConfig: UrlConfig): String = {
    edition match {
      case UK => "/digital/country"
      case DigitalEdition(_, _, _, campaign) => urlConfig.externalSubscriptionUrl + "?prom=DGA38&CMP=" + campaign
    }
  }
}
