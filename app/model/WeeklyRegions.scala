package model

import com.gu.memsub.subsv2.Catalog
import com.netaporter.uri.Uri

trait WeeklyRegion {
  def title: String

  def description: String

  def url: Uri
}

object WeeklyRegion {
  def all(catalog: Catalog): List[WeeklyRegion] = List(UnitedKingdom(catalog), UnitedStates(catalog), Row(catalog))
}

case class UnitedKingdom(catalog: Catalog) extends WeeklyRegion {
  override val title = "United Kingdom"
  override val description = "Includes Isle of Man and Channel Islands"
  override val url = Uri.parse(s"checkout/${catalog.weeklyZoneA.quarter.slug}").addParam("countryGroup", "uk")
}

case class UnitedStates(catalog: Catalog) extends WeeklyRegion {
  override val title = "United States"
  override val description = "Includes Alaska and Hawaii"
  override val url = Uri.parse(s"checkout/${catalog.weeklyZoneA.quarter.slug}").addParam("countryGroup", "us")
}

case class Row(catalog: Catalog) extends WeeklyRegion {
  override val title = "Rest of the world"
  override val description = "Posted to you by air mail"
  override val url = Uri.parse(s"checkout/${catalog.weeklyZoneC.quarter.slug}")
}
