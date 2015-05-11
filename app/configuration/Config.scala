package configuration

import com.gu.googleauth.GoogleAuthConfig
import com.typesafe.config.ConfigFactory

object Config {
  val googleAuthConfig = {
    val GuardianGoogleAppsDomain = "guardian.co.uk"
    val con = ConfigFactory.load().getConfig("google.oauth")
    GoogleAuthConfig(
      con.getString("client.id"),
      con.getString("client.secret"),
      con.getString("callback"),
      Some(GuardianGoogleAppsDomain) // Google App domain to restrict login
    )
  }
}
