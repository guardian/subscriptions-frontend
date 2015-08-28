package utils

object Dates {
  
  def getOrdinalDay(day: Int):String = {
    val suffix = daySuffix(day)
    s"$day$suffix"
  }

  def daySuffix(day: Int):String = day match {
    case 11 | 12 | 13 => "th"
    case _ => day % 10 match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
  }

}
