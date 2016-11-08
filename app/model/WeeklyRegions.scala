package model

import com.netaporter.uri.Uri

trait WeeklyRegion {
  def title: String

  def description: String

  def url: Uri
}

object WeeklyRegion {
  val all = List(UnitedKingdom, UnitedStates, Row)
}

object UnitedKingdom extends WeeklyRegion {
  override val title = "United Kingdom"
  override val description = "Includes Isle of Man and Channel Islands"
  override val url = Uri.parse("checkout/weeklyzonea-guardianweeklyquarterly?countryGroup=uk")
}

object UnitedStates extends WeeklyRegion {
  override val title = "United States"
  override val description = "Includes Alaska and Hawaii"
  override val url = Uri.parse("checkout/weeklyzonea-guardianweeklyquarterly?countryGroup=us")
}

object Row extends WeeklyRegion {
  override val title = "Rest of the world"
  override val description = "Posted to you by Air Mail"
  override val url = Uri.parse("checkout/weeklyzoneb-guardianweeklyquarterly")
}

