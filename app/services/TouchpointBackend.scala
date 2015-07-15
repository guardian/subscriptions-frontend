package services

import configuration.Config
import touchpoint.{ProductRatePlan, TouchpointBackendConfig}

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val ratePlan = touchpointBackendConfig.productRatePlan

    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, ratePlan)

    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)

    TouchpointBackend(salesforceRepo, zuoraService, ratePlan)
  }

  lazy val Normal = TouchpointBackend(BackendType.Default)

  lazy val All = Seq(Normal)
}

case class TouchpointBackend(
  salesforceRepo: SalesforceRepo,
  zuoraService: ZuoraService,
  ratePlan: ProductRatePlan) {

  def start() = {
    salesforceRepo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}
