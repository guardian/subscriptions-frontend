import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.{EssentialAction, EssentialFilter, WithFilters}
import play.filters.csrf.CSRFFilter
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}

object Global extends WithFilters(
  HandleXFrameOptionsOverrideHeader,
  CheckCacheHeadersFilter,
  SecurityHeadersFilter(SecurityHeadersConfig(
    frameOptions=Some("SAMEORIGIN"),
    permittedCrossDomainPolicies = None,
    contentSecurityPolicy = None
  )),
  new ExcludingCSRFFilter(CSRFFilter()),
  AddGuIdentityHeaders,
  AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()
  }
}

// taken from http://dominikdorn.com/2014/07/playframework-2-3-global-csrf-protection-disable-csrf-selectively/
// play 2.5 removes the need for this by considering trusted CORS routes exempt from CSRF
private class ExcludingCSRFFilter(filter: CSRFFilter) extends EssentialFilter {

  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    import play.api.mvc._

    override def apply(rh: RequestHeader) = {
      val chainedFilter = filter.apply(nextFilter)
      if (rh.tags.getOrElse("ROUTE_COMMENTS", "").contains("NOCSRF")) {
        nextFilter(rh)
      } else {
        chainedFilter(rh)
      }
    }
  }
}