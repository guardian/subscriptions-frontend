import filters.{AddEC2InstanceHeader, CheckCacheHeadersFilter}
import play.filters.csrf.CSRFFilter
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}
import services.TouchpointBackend

object Global extends WithFilters(
  CheckCacheHeadersFilter,
  SecurityHeadersFilter(SecurityHeadersConfig(
    frameOptions=Some("SAME-ORIGIN"),
    permittedCrossDomainPolicies = None,
    contentSecurityPolicy = None
  )),
  CSRFFilter(),
  AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()
    TouchpointBackend.All.foreach(_.start())
  }
}
