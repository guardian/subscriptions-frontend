package model

import org.joda.time.LocalTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._

case class SupportPhoneDetails(open: LocalTime, duration: Duration, phoneNumber: String)

object SupportPhoneDetails {
  object UK extends SupportPhoneDetails(new LocalTime(8,0),12.hours,"0330 333 6767")
}
