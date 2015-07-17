package configuration

import com.typesafe.config.ConfigFactory
import play.api.mvc.Cookie

object QA {
  private val config = ConfigFactory.load()

  val passthroughCookie = Cookie(
    name = "qa-passthrough",
    value = config.getString("qa.passthrough-cookie-value")
  )
}
