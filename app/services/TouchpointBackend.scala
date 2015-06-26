package services

import configuration.Config
import touchpoint.TouchpointBackendConfig

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val zuoraRepo = new ZuoraRepo(touchpointBackendConfig.zuora)

    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)

    TouchpointBackend(salesforceRepo, zuoraRepo)
  }

  val Normal = TouchpointBackend(BackendType.Default)

  val All = Seq(Normal)
}

case class TouchpointBackend(
  salesforceRepo: SalesforceRepo,
  zuoraRepo : ZuoraRepo) {

  def start() = {
    salesforceRepo.salesforce.authTask.start()
    zuoraRepo.authTask.start()
  }
}