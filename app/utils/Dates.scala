package utils

object Dates {
  def getOrdinalDay(day: Int): String = {
    val last = day % 10

    val suffix = day match {
      case teens if 11 to 13 contains teens => "th"
      case x if last equals 1 => "st"
      case x if last equals 2 => "nd"
      case x if last equals 3 => "rd"
      case _ => "th"
    }
    s"$day$suffix"
  }
}
