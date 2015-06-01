package configuration

import com.gu.googleauth.GoogleAuthConfig
import com.typesafe.config.ConfigFactory
import net.kencochrane.raven.dsn.Dsn

import scala.util.Try

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

  val config = ConfigFactory.load()

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"

  val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

}
