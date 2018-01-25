package services

import configuration.Config
import model.DigitalEdition
import model.promoCodes.{GuardianWeekly, _}
import org.joda.time.DateTime

object FlashSale {

  def inOfferPeriod(promoCodeKey: PromoCodeKey) = {
    //The offer is valid between 29th Jan 2018 & 25th Feb 2018
    //The current sale is paper & paper + digital only, digital is unaffected
    val included: Map[PromoCodeKey, Boolean] = Map(
      Digital -> false,
      Paper -> true,
      PaperAndDigital -> true
    )

    val startTime = new DateTime(2018, 1, 29, 0, 0)
    val endTime = new DateTime(2018, 2, 25, 0, 0)
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
      PaperAndDigital -> getCode(PaperAndDigital, s"NHOME${edition.id.toUpperCase}D", "GRB80X"),
      Paper -> getCode(Paper, s"NHOME${edition.id.toUpperCase}P", "GRB80P"),
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )

  def offersPromoCodes: Map[PromoCodeKey, String] = offersPromoCodes(DigitalEdition.UK)

  def offersPromoCodes(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> getCode(Digital, s"DOFF${edition.id.toUpperCase}1", "DBR80G"),
      PaperAndDigital -> getCode(PaperAndDigital, s"NOFF${edition.id.toUpperCase}D", "GRB80X"),
      Paper -> getCode(Paper, s"NOFF${edition.id.toUpperCase}P", "GRB80P"),
      GuardianWeekly -> "WAL41X"
    )

  private def getCode(promoCodeKey: PromoCodeKey, defaultCode: String, offerCode: String) =
    if (inOfferPeriod(promoCodeKey))
      offerCode
    else
      defaultCode
}
