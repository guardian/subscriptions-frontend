package services

import configuration.Config
import model.DigitalEdition
import model.promoCodes.{GuardianWeekly, _}
import org.joda.time.DateTime

object FlashSale {

  def inOfferPeriod(promoCodeKey: PromoCodeKey) = {
    //The offer is valid between 15th May 2018 & 29th May 2018
    //The current sale is paper & paper + digital only, digital is unaffected
    val included: Map[PromoCodeKey, Boolean] = Map(
      Digital -> false,
      Paper -> true,
      PaperAndDigital -> true
    )

    val startTime = new DateTime(2018, 5, 15, 0, 0)
    val endTime = new DateTime(2018, 5, 29, 0, 0)
    val now = new DateTime()

    now.isAfter(startTime) &&
      now.isBefore(endTime) &&
      included(promoCodeKey) ||
      (included(promoCodeKey) && !Config.stageProd) //allow testing on CODE
  }

  def homePromoCodes: Map[PromoCodeKey, String] = homePromoCodes(DigitalEdition.UK)

  def homePromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> getCode(Digital, s"DHOME${edition.id.toUpperCase}1", "DBR80F"),
      PaperAndDigital -> getCode(PaperAndDigital, s"NHOME${edition.id.toUpperCase}D", "GST80K"),
      Paper -> getCode(Paper, s"NHOME${edition.id.toUpperCase}P", "GST80J"),
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )

  def offersPromoCodes: Map[PromoCodeKey, String] = offersPromoCodes(DigitalEdition.UK)

  def offersPromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> getCode(Digital, s"DOFF${edition.id.toUpperCase}1", "DBR80G"),
      PaperAndDigital -> getCode(PaperAndDigital, s"NOFF${edition.id.toUpperCase}D", "GST80I"),
      Paper -> getCode(Paper, s"NOFF${edition.id.toUpperCase}P", "GST80H"),
      GuardianWeekly -> "WAL41X"
    )

  private def getCode(promoCodeKey: PromoCodeKey, defaultCode: String, offerCode: String) =
    if (inOfferPeriod(promoCodeKey))
      offerCode
    else
      defaultCode
}
