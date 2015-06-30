import filters.CheckCacheHeadersFilter
import play.filters.csrf.CSRFFilter
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import services.TouchpointBackend

object Global extends WithFilters(CheckCacheHeadersFilter, CSRFFilter()) {
  override def onStart(app: Application) {
    SentryLogging.init()
    TouchpointBackend.All.foreach(_.start())
  }
}
