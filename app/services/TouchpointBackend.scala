package services

import configuration.Config
import play.api.mvc.RequestHeader
import touchpoint.TouchpointBackendConfig
import utils.TestUsers._

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {
    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)
    val salesforceService = new SalesforceServiceImp(salesforceRepo)
    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, touchpointBackendConfig.digitalProductPlan, touchpointBackendConfig.zuoraProperties)

    TouchpointBackend(salesforceService, zuoraService)
  }

  val Normal = TouchpointBackend(BackendType.Default)
  val TestUser = TouchpointBackend(BackendType.Testing)

  val All = Seq(Normal, TestUser)

  def forRequest(alternateSource: => Option[String] = None)(implicit request: RequestHeader) =
    if (isTestUser(alternateSource)) TestUser else Normal
}

case class TouchpointBackend(
  salesforceService: SalesforceService,
  zuoraService : ZuoraService
) {

  val checkoutService = new CheckoutService(IdentityService, salesforceService, zuoraService, ExactTargetService)

  def start() = {
    salesforceService.repo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}
