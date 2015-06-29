package services

import com.gu.identity.play.IdMinimalUser
import configuration.Config
import touchpoint.TouchpointBackendConfig

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val zuoraService = new ZuoraService(touchpointBackendConfig.zuora)
    
    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)

    TouchpointBackend(salesforceRepo, zuoraService)
  }

  val Normal = TouchpointBackend(BackendType.Default)

  val All = Seq(Normal)

  //TODO when implementing test-users this requires updating to supply data to correct location
  def forUser(user: IdMinimalUser) = Normal
}

case class TouchpointBackend(
  salesforceRepo: SalesforceRepo,
  zuoraService : ZuoraService) {

  def start() = {
    salesforceRepo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}