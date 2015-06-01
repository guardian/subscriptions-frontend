import filters.CheckCacheHeadersFilter
import play.api.Application
import play.api.mvc.WithFilters
import services.SalesforceRepo

object Global extends WithFilters(CheckCacheHeadersFilter) {
  override def onStart(app: Application) = SalesforceRepo.salesforce.authTask.start()
}
