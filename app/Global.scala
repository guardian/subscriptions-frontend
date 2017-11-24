import configuration.Config
import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import loghandling.Logstash
import monitoring.SentryLogging
import play.api.GlobalSettings
import play.api.Application
import play.api.mvc.{EssentialAction, EssentialFilter, WithFilters}
import play.filters.csrf.CSRFFilter
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    SentryLogging.init()
    Logstash.init(Config)
  }
}
