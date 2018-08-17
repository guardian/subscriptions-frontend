package services

import configuration.Config
import model.DigitalEdition
import model.promoCodes.{GuardianWeekly, PaperAndDigital, _}
import org.joda.time.DateTime

object FlashSale extends FlashSale {

  val startTime = new DateTime(2018, 8, 20, 0, 0)
  val endTime = new DateTime(2018, 9, 3, 0, 0)

  val flashSaleIntcmp: Map[String, String] =
    Map(
      "GFS80H" -> "gdnwb_macqn_other_subs_SupporterLandingPagePrintOnlySubscribeLandingPagePrint+digital_",
      "GFS80F" -> "gdnwb_macqn_other_subs_guardianarticleSubscribeLandingPagePrintOnly_",
      "GFS80K" -> "gdnwb_macqn_other_subs_SubPromotionsLandingPagePrintOnlySubsPromotionsLandingPagePrint+digital_",
      "GFS80J" -> "gdnwb_macqn_other_subs_SupporterLandingPagePrint+digitalSubPromotionsLandingPagePrintOnly_"
    )
}

trait FlashSale {

  val startTime: DateTime
  val endTime: DateTime

  val flashSaleIntcmp: Map[String, String]

  //The offer is valid between 20th August 2018 & 2nd September 2018
  //The current sale is paper & paper + digital only. Digital only is unaffected.
  val included: Map[PromoCodeKey, Boolean] = Map(
    Digital -> false,
    Paper -> true,
    PaperAndDigital -> true
  )

  private def inFlashSaleDateRange: Boolean = {
    val now = new DateTime()

    now.isAfter(startTime) && now.isBefore(endTime)
  }

  def inOfferPeriod(promoCodeKey: PromoCodeKey) = {
    inFlashSaleDateRange &&
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
      PaperAndDigital -> getCode(PaperAndDigital, s"NOFF${edition.id.toUpperCase}D", "GFS80K"),
      Paper -> getCode(Paper, s"NOFF${edition.id.toUpperCase}P", "GFS80J"),
      GuardianWeekly -> "WAL41X"
    )

  private def getCode(promoCodeKey: PromoCodeKey, defaultCode: String, offerCode: String) =
    if (inOfferPeriod(promoCodeKey))
      offerCode
    else
      defaultCode

  def intcmp(promoCode: String): String =
    if(inFlashSaleDateRange)
      flashSaleIntcmp.getOrElse(promoCode, defaultIntcmp(promoCode))
    else
      defaultIntcmp(promoCode)


  private def defaultIntcmp(promoCode: String) = s"FROM_P_${promoCode}"
}
