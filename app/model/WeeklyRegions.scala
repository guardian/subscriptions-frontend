package model

import com.netaporter.uri.Uri

trait WeeklyRegion {
  def title: String

  def description: String

  def url: Uri
}

object WeeklyRegion {
  val all = List(Uk, NorthAmerica, Row)
}

object Uk extends WeeklyRegion {
  override val title = "United Kingdom"
  override val description = "Includes Isle of Man and Channel Islands"
  override val url = Uri.parse("checkout/weeklyzonea-guardianweeklyquarterly?countryGroup=uk")
}

object NorthAmerica extends WeeklyRegion {
  override val title = "North America"
  override val description = "Canada and USA"
  override val url = Uri.parse("checkout/weeklyzonea-guardianweeklyquarterly?countryGroup=us")
}

object Row extends WeeklyRegion {
  override val title = "Rest of the world"
  override val description = "Posted to you by Air Mail"
  override val url = Uri.parse("checkout/weeklyzoneb-guardianweeklyquarterly")
}

