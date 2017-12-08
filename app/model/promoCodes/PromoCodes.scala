package model.promoCodes

import model.DigitalEdition

object PromoCodes {

  def home: Map[PromoCodeKey, String] = home(DigitalEdition.UK)

  def home(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> s"DHOME${edition.id.toUpperCase}1",
      PaperAndDigital -> s"NHOME${edition.id.toUpperCase}D",
      Paper -> s"NHOME${edition.id.toUpperCase}P",
      GuardianWeekly -> s"WHOME${edition.id.toUpperCase}"
    )

  def offers: Map[PromoCodeKey, String] = offers(DigitalEdition.UK)

  def offers(edition: DigitalEdition): Map[PromoCodeKey, String] =
    Map(
      Digital -> s"DOFF${edition.id.toUpperCase}1",
      PaperAndDigital -> s"NOFF${edition.id.toUpperCase}D",
      Paper -> s"NOFF${edition.id.toUpperCase}P",
      GuardianWeekly -> "WAL41X"
    )
}
