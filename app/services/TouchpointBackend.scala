package services

import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import touchpoint.TouchpointBackendConfig
import utils.TestUsers._

object TouchpointBackend extends LazyLogging{
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.backendType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {
    val salesforceRepo = new SalesforceRepo(touchpointBackendConfig.salesforce)
    val salesforceService = new SalesforceServiceImp(salesforceRepo)
    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, touchpointBackendConfig.digitalProductPlan)


    TouchpointBackend(salesforceService, zuoraService)
  }

  val Normal = TouchpointBackend(BackendType.Default)
  val TestUser = TouchpointBackend(BackendType.Testing)


  val All = Seq(Normal, TestUser)

  def forUser(username: String) = if (isTestUser(username)) TestUser else Normal
}

case class TouchpointBackend(
  salesforceService: SalesforceService,
  zuoraService : ZuoraService
) {

  def start() = {
    salesforceService.repo.salesforce.authTask.start()
    zuoraService.authTask.start()
  }
}
