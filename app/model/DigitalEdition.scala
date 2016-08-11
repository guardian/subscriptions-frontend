package model
import com.gu.i18n.CountryGroup

case class DigitalEdition(countryGroup: CountryGroup, id: String, name: String, campaign: String)

object DigitalEdition {
  object UK extends DigitalEdition(CountryGroup.UK, "uk", "UK", "dis_2408")
  object US extends DigitalEdition(CountryGroup.US, "us", "US", "dis_2378")
  object INT extends DigitalEdition(CountryGroup.RestOfTheWorld, "int", "International", "dis_2378")
  object AU extends DigitalEdition(CountryGroup.Australia, "au", "Australia", "dis_2379")

  def getById(id: String): Option[DigitalEdition] = {
    id match {
      case UK.id => Some(UK)
      case US.id => Some(US)
      case AU.id => Some(AU)
      case INT.id => Some(INT)
      case _ => None
    }
  }

  def getForCountryGroup(countryGroup: CountryGroup) = getById(countryGroup.id).getOrElse(INT)
}
