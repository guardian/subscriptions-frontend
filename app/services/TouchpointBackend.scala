package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.touchpoint.TouchpointBackendConfig.BackendType
import configuration.Config
import touchpoint.TouchpointBackendConfig
import utils.TestUsers._

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
 // val TestUser = TouchpointBackend(BackendType.Testing)


  val All = Seq(Normal)

  //def forUser(user: IdMinimalUser) = if (isTestUser(user)) TestUser else Normal
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
