import javax.inject.Inject

import akka.stream.Materializer
import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject()(
                         handleXFrameOptionsOverrideHeader: HandleXFrameOptionsOverrideHeader,
                         checkCacheHeadersFilter: CheckCacheHeadersFilter,
                         securityHeadersFilter: SecurityHeadersFilter,
                         cSRFFilter: CSRFFilter,
                         addGuIdentityHeaders: AddGuIdentityHeaders,
                         addEC2InstanceHeader: AddEC2InstanceHeader)  (implicit  val mat: Materializer) extends DefaultHttpFilters(handleXFrameOptionsOverrideHeader,checkCacheHeadersFilter,securityHeadersFilter,cSRFFilter, addGuIdentityHeaders, addEC2InstanceHeader)
