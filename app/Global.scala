import filters.CheckCacheHeadersFilter
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import services.SalesforceRepo

object Global extends WithFilters(CheckCacheHeadersFilter) {
  override def onStart(app: Application) {
    SentryLogging.init()
    SalesforceRepo.salesforce.authTask.start()
  }
}
