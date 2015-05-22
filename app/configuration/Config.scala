package configuration

import com.gu.googleauth.GoogleAuthConfig
import com.typesafe.config.ConfigFactory

object Config {
  val config = ConfigFactory.load()

  val googleAuthConfig = {
    val con = ConfigFactory.load().getConfig("google.oauth")
    GoogleAuthConfig(
      clientId = con.getString("client.id"),
      clientSecret = con.getString("client.secret"),
      redirectUrl = con.getString("callback"),
      domain = Some("guardian.co.uk") // Google App domain to restrict login
    )
  }

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"

  object Identity {
    val idConfig = config.getConfig("identity")

    val root = idConfig.getString("root")
  }
}
