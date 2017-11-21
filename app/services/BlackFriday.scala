package services

import configuration.Config
import model.DigitalEdition
import model.promoCodes.{GuardianWeekly, _}
import org.joda.time.DateTime

//TODO: Remove after 3rd December 2017
object BlackFriday {

  def inOfferPeriod = {
    //The offer is valid between 24th November & 3rd December 2017
    val startTime = new DateTime(2017, 11, 24, 0, 0)
    val endTime = new DateTime(2017, 12, 4, 0, 0)
    val now = new DateTime()
    now.isAfter(startTime) && now.isBefore(endTime) //|| !Config.stageProd //allow testing on CODE
  }

  def homePromoCodes: Map[PromoCodeKey, String] = homePromoCodes(DigitalEdition.UK)

  def homePromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] = if (inOfferPeriod) {
    Map(
      Digital -> "DBQ80F",
      PaperAndDigital -> "GBQ80H",
      Paper -> "GBQ80G",
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )
  }
  else {
    Map(
      Digital -> s"DHOME${edition.id.toUpperCase}1",
      PaperAndDigital -> s"NHOME${edition.id.toUpperCase}D",
      Paper -> s"NHOME${edition.id.toUpperCase}P",
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )
  }

  def offersPromoCodes: Map[PromoCodeKey, String] = offersPromoCodes(DigitalEdition.UK)

  def offersPromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] = if (inOfferPeriod) {
    Map(
      Digital -> "DBQ80J",
      PaperAndDigital -> "GBQ80L",
      Paper -> "GBQ80K",
      GuardianWeekly -> "WAL41X"
    )
  } else {
    Map(
      Digital -> s"DOFF${edition.id.toUpperCase}1",
      PaperAndDigital -> s"NOFF${edition.id.toUpperCase}D",
      Paper -> s"NOFF${edition.id.toUpperCase}P",
      GuardianWeekly -> "WAL41X"
    )
  }
}
