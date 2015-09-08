package utils

import org.scalatest.FreeSpec

class DatesSpec extends FreeSpec {
  "Date Formatters" - {
    "Ordinal day formatting" in {
      assert(Dates.getOrdinalDay(11) == "11th")
      assert(Dates.getOrdinalDay(12) == "12th")
      assert(Dates.getOrdinalDay(13) == "13th")
      assert(Dates.getOrdinalDay(21) == "21st")
      assert(Dates.getOrdinalDay(7) == "7th")
      assert(Dates.getOrdinalDay(1) == "1st")
      assert(Dates.getOrdinalDay(2) == "2nd")
      assert(Dates.getOrdinalDay(3) == "3rd")
    }
  }
}
