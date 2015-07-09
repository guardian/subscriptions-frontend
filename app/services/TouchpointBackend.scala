package services

import com.gu.identity.play.IdMinimalUser
import configuration.Config
import touchpoint.{ProductRatePlan, TouchpointBackendConfig}

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val zuoraService = new ZuoraService(touchpointBackendConfig.zuora)

    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)

    val ratePlan = touchpointBackendConfig.productRatePlan

    TouchpointBackend(salesforceRepo, zuoraService, ratePlan)
  }

  val Normal = TouchpointBackend(BackendType.Default)

  val All = Seq(Normal)
}

case class TouchpointBackend(
  salesforceRepo: SalesforceRepo,
  zuoraService : ZuoraService,
  ratePlans: Seq[ProductRatePlan]) {

  def start() = {
    salesforceRepo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}
