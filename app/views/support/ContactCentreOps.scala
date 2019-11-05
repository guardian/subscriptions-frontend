package views.support

import com.gu.i18n.Country
import com.gu.i18n.Country.UK

object ContactCentreOps {

  private val countriesHandledByUSCallCentre = Set(Country.Canada, Country.US)            // Improvement: Add South America countries
  private val countriesHandledByAUCallCentre = Set(Country.Australia, Country.NewZealand) // Improvement: Add Asia/Pacific countries

  def phone(contactUsCountry: Country): String = phone(Some(contactUsCountry))

  def phone(contactUsCountry: Option[Country] = None): String = {
    if (contactUsCountry exists countriesHandledByUSCallCentre)
      s"1-844-632-2010 (toll free); ${directLine(contactUsCountry)} (direct line). Lines are open 9am - 5pm on weekdays (EST/EDT)"
    else if (contactUsCountry exists countriesHandledByAUCallCentre)
      s"1800 773 766 (within Australia & toll free); ${directLine(contactUsCountry)} (direct line/outside Australia). Lines are open 9am-5pm, Monday to Friday (AEDT) excluding public holidays"
    else if (contactUsCountry.contains(UK))
      s"${directLine(UK)} (within UK). Lines are open 8am-8pm on weekdays, 8am-6pm at weekends (GMT/BST)"
    else
      s"${directLine()}. Lines are open 8am-8pm on weekdays, 8am-6pm at weekends (GMT/BST)"
  }

  def directLine(contactUsCountry: Country): String = directLine(Some(contactUsCountry))

  def directLine(contactUsCountry: Option[Country] = None): String = {
    if (contactUsCountry exists countriesHandledByUSCallCentre)
      "917-900-4663"
    else if (contactUsCountry exists countriesHandledByAUCallCentre)
      "+61 2 8076 8599"
    else if (contactUsCountry contains UK)
      "0330 333 6767"
    else
      "+44 (0) 330 333 6767"
  }

  def hrefTelNumber(contactUsCountry: Country): String = hrefTelNumber(Some(contactUsCountry))

  def hrefTelNumber(contactUsCountry: Option[Country] = None): String = {
    s"tel:${directLine(contactUsCountry).replace(" (0) "," ")}"
  }

  def hrefMailto = "mailto:subscriptions@theguardian.com"

  def email = "subscriptions@theguardian.com"

  private val auHelpEmail = "apac.help@theguardian.com"

  def weeklyEmail(contactUsCountry: Option[Country]): String = {
    contactUsCountry match {
      case c: Some[Country] if c exists countriesHandledByAUCallCentre => auHelpEmail
      case c: Some[Country] if c exists countriesHandledByUSCallCentre => "northamerica.help@theguardian.com"
      case _ => "customer.help@theguardian.com"
    }
  }

  def digiPackEmail(contactUsCountry: Option[Country]): String = {
    contactUsCountry match {
      case c: Some[Country] if c exists countriesHandledByAUCallCentre => auHelpEmail
      case _ => "digitalpack@theguardian.com"
    }
  }
}
