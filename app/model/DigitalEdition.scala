package model

case class DigitalEdition(id: String, name: String, price: String, cmp: String)


object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

  def getRedirect(e: DigitalEdition): String = {
    e match {
      case UK => "/digital/country"
      case DigitalEdition(id, name, price, cmp) => "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=DGA38&CMP=" + cmp
    }
  }
}
