package services

import configuration.Config
import touchpoint.TouchpointBackendConfig

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {
    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)
    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, touchpointBackendConfig.digitalProductPlan)

    TouchpointBackend(salesforceRepo, zuoraService)
  }

  val Normal = TouchpointBackend(BackendType.Default)

  val All = Seq(Normal)
}

case class TouchpointBackend(
  salesforceRepo: SalesforceRepo,
  zuoraService : ZuoraService
) {

  def start() = {
    salesforceRepo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}
