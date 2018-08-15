package services

import configuration.Config
import model.DigitalEdition
import model.promoCodes.{GuardianWeekly, _}
import org.joda.time.DateTime

object FlashSale {

  def inOfferPeriod(promoCodeKey: PromoCodeKey) = {
    //The offer is valid between 20th August 2018 & 2nd September 2018
    //The current sale is paper & paper + digital only. Digital only is unaffected.
    val included: Map[PromoCodeKey, Boolean] = Map(
      Digital -> false,
      Paper -> true,
      PaperAndDigital -> true
    )

    val startTime = new DateTime(2018, 8, 20, 0, 0)
    val endTime = new DateTime(2018, 9, 3, 0, 0)
    val now = new DateTime()

    now.isAfter(startTime) &&
      now.isBefore(endTime) &&
      included(promoCodeKey) ||
      (included(promoCodeKey) && !Config.stageProd) //allow testing on CODE
  }

  def homePromoCodes: Map[PromoCodeKey, String] = homePromoCodes(DigitalEdition.UK)

  def homePromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> getCode(Digital, s"DHOME${edition.id.toUpperCase}1", "DPS80P"),
      PaperAndDigital -> getCode(PaperAndDigital, s"NHOME${edition.id.toUpperCase}D", "GFS80H"),
      Paper -> getCode(Paper, s"NHOME${edition.id.toUpperCase}P", "GFS80F"),
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )

  def offersPromoCodes: Map[PromoCodeKey, String] = offersPromoCodes(DigitalEdition.UK)

  def offersPromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> getCode(Digital, s"DOFF${edition.id.toUpperCase}1", "DPS80P"),
      PaperAndDigital -> getCode(PaperAndDigital, s"NOFF${edition.id.toUpperCase}D", "GFS80J"),
      Paper -> getCode(Paper, s"NOFF${edition.id.toUpperCase}P", "GFS80K"),
      GuardianWeekly -> "WAL41X"
    )

  private def getCode(promoCodeKey: PromoCodeKey, defaultCode: String, offerCode: String) =
    if (inOfferPeriod(promoCodeKey))
      offerCode
    else
      defaultCode
}
