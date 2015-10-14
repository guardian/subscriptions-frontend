package services

import com.gu.membership.stripe.StripeService
import com.gu.monitoring.StatusMetrics
import configuration.Config
import monitoring.TouchpointBackendMetrics
import play.api.mvc.RequestHeader
import touchpoint.TouchpointBackendConfig
import touchpoint.TouchpointBackendConfig.BackendType
import utils.TestUsers._

object TouchpointBackend {

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {
    val touchpointBackendConfig = TouchpointBackendConfig.backendType(backendType, Config.config)
    val salesforceService = new SalesforceServiceImp(new SalesforceRepo(touchpointBackendConfig.salesforce))
    val zuoraService = new ZuoraApiClient(touchpointBackendConfig.zuora, touchpointBackendConfig.digitalProductPlan, touchpointBackendConfig.zuoraProperties)
    val stripeServiceInstance = new StripeService(touchpointBackendConfig.stripe, new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = touchpointBackendConfig.stripe.envName
      val service = "Stripe"
    })
    val paymentService = new PaymentService {
      override def stripeService = stripeServiceInstance
    }

    TouchpointBackend(
      touchpointBackendConfig.environmentName,
      salesforceService,
      zuoraService,
      paymentService
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
  zuoraService : ZuoraService,
  paymentService: PaymentService
) {
  private val zuoraServiceInstance = zuoraService
  private val exactTargetService = new ExactTargetService {
    override def zuoraService = zuoraServiceInstance
  }
  val checkoutService = new CheckoutService(IdentityService, salesforceService, paymentService, zuoraService, exactTargetService)
}
