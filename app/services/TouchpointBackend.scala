package services

import configuration.Config
import play.api.mvc.RequestHeader
import touchpoint.TouchpointBackendConfig
import touchpoint.TouchpointBackendConfig.BackendType
import utils.TestUsers._

object TouchpointBackend {

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {
    val touchpointBackendConfig = TouchpointBackendConfig.backendType(backendType, Config.config)
    val salesforceService = new SalesforceServiceImp(new SalesforceRepo(touchpointBackendConfig.salesforce))
    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, touchpointBackendConfig.digitalProductPlan, touchpointBackendConfig.zuoraProperties)

    TouchpointBackend(
      touchpointBackendConfig.environmentName,
      salesforceService,
      zuoraService
    )
  }

  val BackendsByType = BackendType.All.map(typ => typ -> TouchpointBackend(typ)).toMap

  val Normal = BackendsByType(BackendType.Default)

  val All = BackendsByType.values.toSeq

  case class Resolution(
    backend: TouchpointBackend,
    typ: TouchpointBackendConfig.BackendType,
    validTestUserCredentialOpt: Option[TestUserCredentialType[_]]
  )

  /**
   * Alternate credentials are used *only* when the user is not signed in - if you're logged in as
   * a 'normal' non-test user, it doesn't make any difference what pre-signin-test-cookie you have.
   */
  def forRequest[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C)(
    implicit request: RequestHeader): Resolution = {
    val validTestUserCredentialOpt = isTestUser(permittedAltCredentialType, altCredentialSource)
    val backendType = if (validTestUserCredentialOpt.isDefined) BackendType.Testing else BackendType.Default
    Resolution(BackendsByType(backendType), backendType, validTestUserCredentialOpt)
  }
}

case class TouchpointBackend(
  environmentName: String,
  salesforceService: SalesforceService,
  zuoraService : ZuoraService
) {

  val checkoutService = new CheckoutService(IdentityService, salesforceService, zuoraService, ExactTargetService)

  def start() = {
    salesforceService.repo.salesforce.authTask.start()
  }
}
