package views.support

import com.github.nscala_time.time.Imports._
import configuration.Config.timezone
import org.joda.time.format.PeriodFormat
import org.joda.time.{Instant, Interval, PeriodType}
import play.twirl.api.Html

object Dates {

  val humanPeriodFormat = PeriodFormat.getDefault

  val YearMonthDayHours = PeriodType.yearMonthDayTime.withMinutesRemoved.withSecondsRemoved.withMillisRemoved

  implicit class RichPeriod(period: Period) {
    lazy val pretty = period.withMillis(0).toString(humanPeriodFormat)
  }

  implicit class RichDateTime(dt: DateTime) {
    lazy val pretty = prettyDate(dt)
    lazy val prettyWithTime = prettyDateWithTime(dt)
  }

  implicit class RichLocalDate(ld: LocalDate) {
    val dt = ld.toDateTimeAtCurrentTime()
    lazy val pretty = prettyDate(dt)
    lazy val prettyWithTime = prettyDateWithTime(dt)
    lazy val shortMonth: String = dt.toString("MMM YYYY")
    lazy val shortDay: String = dt.toString("EEE")

  }

  implicit class RichInstant(dt: Instant) {
    lazy val pretty = prettyDate(new DateTime(dt))
    lazy val prettyWithoutYear = prettyDateNoYear(new DateTime(dt))
    lazy val prettyWithTime = prettyDateWithTime(new DateTime(dt))
    lazy val prettyNoYearWithTime = prettyDateNoYearTime(new DateTime(dt))

    def pretty(includeTime: Boolean): String = if (includeTime) prettyWithTime else pretty

    def prettyWithoutYear(includeTime: Boolean): String = if (includeTime) prettyNoYearWithTime else prettyWithoutYear

    def isContemporary(threshold: Duration = 24.hours) = new Interval(dt - threshold, dt + threshold).contains(DateTime.now)
  }

  implicit class RichInterval(interval: Interval) {
    lazy val isSameDay = interval.start.withTimeAtStartOfDay() == interval.end.withTimeAtStartOfDay()
  }

  def prettyDate(dt: DateTime): String = dt.toString("d MMMMM YYYY")

  def prettyDate(dt: LocalDate): String = dt.toString("d MMMMM YYYY")

  def prettyDateNoYear(dt: DateTime): String = dt.toString("d MMMMM")

  def prettyDateNoYear(dt: LocalDate): String = dt.toString("d MMMMM")

  def prettyTime(dt: DateTime): String =
    dt.toString(if (dt.getMinuteOfHour == 0) "h" else "h.mm") +dt.toString("a").toLowerCase

  def prettyDateWithTime(dt: DateTime): String = prettyDate(dt) + ", " + prettyTime(dt)

  def prettyDateNoYearTime(dt: DateTime): String = prettyDateNoYear(dt) + ", " + prettyTime(dt)

  def prettyDateWithTimeAndDayName(dt: DateTime): String =
    dt.toString("EEEE ") + prettyDateWithTime(dt)

  def prettyShortDateWithTimeAndDayName(dt: DateTime): String =
    dt.toString("EE d MMMMM") + ", " + prettyTime(dt)

  def prettyDateAndDayName(dt: DateTime): String =
    dt.toString("EE ") + prettyDate(dt)

  def prettyLegalDate(dt: DateTime): String =
    dt.withZone(timezone).toString("HH:mm z 'on' d MMMM YYYY") // e.g. 00:00 GMT on 25 December 2016

  def dayInMonthWithSuffix(date: DateTime = DateTime.now): Html = addSuffix(date.toString("dd").toInt)

  def dayInMonthWithSuffixAndMonth(date: DateTime = DateTime.now): Html = {
    Html(addSuffix(date.toString("dd").toInt) + " " + date.toString("MMMM"))
  }

  def dayInMonthWithSuffixAndMonth(date: LocalDate): Html = {
    Html(addSuffix(date.toString("dd").toInt) + " " + date.toString("MMMM"))
  }

  def addSuffix(day: Int): Html = Html(day + "<sup>" + suffix(day) + "</sup>")

  def suffix(day: Int) = day match {
    case 11 | 12 | 13 => "th"
    case _ => day % 10 match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
  }

  case class Range(start: String, end: String)

  private def multiDateRangeFormatter(interval: Interval, fmt: String): Range = {
    def dateToks(dt: DateTime) = dt.toString(fmt).split(" ").reverse

    val startToks = dateToks(interval.start)
    val endToks = dateToks(interval.end)
    val newStartToks = (startToks zip endToks).dropWhile { case (a, b) => a == b }.map { case (a, b) => a }
    Range(newStartToks.reverse.mkString(" "), endToks.reverse.mkString(" "))
  }

  def dateRange(interval: Interval): String = {
    if (interval.isSameDay) {
      prettyDate(interval.start)
    } else {
      val range = multiDateRangeFormatter(interval, "d MMMMM YYYY")
      range.start + "–" + range.end
    }
  }

  def dateTimeRange(interval: Interval): String = {
    if (interval.isSameDay) {
      prettyDateWithTimeAndDayName(interval.start) + "–" + prettyTime(interval.end)
    } else {
      val range = multiDateRangeFormatter(interval, "EEEE d MMMMM YYYY")
      range.start + "–" + range.end
    }
  }

  def formattedDateRange(start: LocalDate, finish: LocalDate) = {
    val formatStart = "d" + (if (start.monthOfYear != finish.monthOfYear) " MMMM" + (if (start.year != finish.year) " YYYY" else "") else "")
    val formatFinish = "d MMMM" + (if (finish.year != LocalDate.now.year) " YYYY" else "")
    val formattedFinish = finish.toString(formatFinish)
    if (finish.isAfter(start)) s"${start.toString(formatStart)} - $formattedFinish" else formattedFinish
  }
}
