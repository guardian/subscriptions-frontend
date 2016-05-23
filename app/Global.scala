import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, AffectXFrameOptionsHeader, CheckCacheHeadersFilter}
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.csrf.CSRFFilter
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}

object Global extends WithFilters(
  AffectXFrameOptionsHeader,
  CheckCacheHeadersFilter,
  SecurityHeadersFilter(SecurityHeadersConfig(
    frameOptions=Some("SAMEORIGIN"),
    permittedCrossDomainPolicies = None,
    contentSecurityPolicy = None
  )),
  CSRFFilter(),
  AddGuIdentityHeaders,
  AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()
  }
}
