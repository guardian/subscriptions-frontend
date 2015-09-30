package model

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, Duration}
import org.scalactic.Tolerance._
import org.scalatest.FreeSpec
import play.api.libs.json.Json

class IdentityCookiesTest extends FreeSpec {
  "After registering a guest user" - {
    "The response we get can be read as identity cookies" in {
      val now = DateTime.now
      val inThreeMonths = now.plus(Duration.standardDays(90L))

      // 2015-12-28T15:22:01+00:00
      val inThreeMonthsStr =ISODateTimeFormat.dateHourMinuteSecond.print(inThreeMonths) + "+00:00"

      val json =
        Json.parse(s"""
          |{
          |  "status": "ok",
          |  "cookies": {
          |    "values": [
          |      {
          |        "key": "GU_U",
          |        "value": "gu_u_value"
          |      },
          |      {
          |        "key": "SC_GU_LA",
          |        "value": "sc_gu_la_value",
          |        "sessionCookie": true
          |      },
          |      {
          |        "key": "SC_GU_U",
          |        "value": "sc_gu_u_value"
          |      }
          |    ],
          |    "expiresAt": "$inThreeMonthsStr"
          |  }
          |}
        """.stripMargin)

        val idCookies = IdentityCookies.fromGuestConversion(json)
        val guCookie = idCookies.map(_.guu)
        val scguCookie = idCookies.map(_.scguu)

        guCookie.fold(fail("Failed to parse GU_U cookie")){ c =>
          assert(c.name === "GU_U")
          assert(c.value === "gu_u_value")
          assert(c.maxAge.get === (90 * 24 * 60 * 60) +- 5)
          assert(!c.secure)
        }

        scguCookie.fold(fail("Failed to parse SC_GU_U cookie")){ c =>
          assert(c.name === "SC_GU_U")
          assert(c.value === "sc_gu_u_value")
          assert(c.maxAge.get === (90 * 24 * 60 * 60) +- 5)
          assert(c.secure)
        }
    }
  }
}
