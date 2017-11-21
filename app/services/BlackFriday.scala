package services

import configuration.Config
import model.promoCodes.{Digital, Paper, PaperAndDigital, PromoCodeKey}
import org.joda.time.DateTime

//TODO: Remove after 3rd December 2017
object BlackFriday {

  def inOfferPeriod = {
    //The offer is valid between 24th November & 3rd December 2017
    val startTime = new DateTime(2017, 11, 24, 0, 0)
    val endTime = new DateTime(2017, 12, 4, 0, 0)
    val now = new DateTime()
    now.isAfter(startTime) && now.isBefore(endTime) || !Config.stageProd //allow testing on CODE
  }

  def homePromoCodes: Map[PromoCodeKey, String] = if (inOfferPeriod) {
    Map(
      Digital -> "DBQ80F",
      PaperAndDigital -> "GBQ80H",
      Paper -> "GBQ80G"
    )
  }
  else {
    Map(
      Digital -> "DHOMEUK1",
      PaperAndDigital -> "NHOMEUKD",
      Paper -> "NHOMEUKP"
    )
  }

  def offersPromoCodes: Map[PromoCodeKey, String] = if (inOfferPeriod) {
    Map(
      Digital -> "DBQ80J",
      PaperAndDigital -> "GBQ80L",
      Paper -> "GBQ80K"
    )
  } else {
    Map(
      Digital -> "DOFFUK1",
      PaperAndDigital -> "NOFFUKD",
      Paper -> "NOFFUKP"
    )
  }
}
