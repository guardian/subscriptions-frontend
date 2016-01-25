package services

import com.gu.config.ProductFamilyRatePlanIds
import com.gu.memsub.Membership
import com.gu.memsub.services.{CatalogService, PromoService, api}
import com.gu.monitoring.{ServiceMetrics, StatusMetrics}
import com.gu.stripe.StripeService
import com.gu.zuora
import com.gu.zuora.{rest, soap}
import configuration.Config
import monitoring.TouchpointBackendMetrics
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import touchpoint.TouchpointBackendConfig.BackendType
import touchpoint.{TouchpointBackendConfig, ZuoraProperties}
import utils.TestUsers._

object TouchpointBackend {

  private implicit val system = Akka.system

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {
    val config = TouchpointBackendConfig.backendType(backendType, Config.config)
    val salesforceService = new SalesforceServiceImp(new SalesforceRepo(config.salesforce))

    val soapClient = new soap.ClientWithFeatureSupplier(Set.empty, config.zuoraSoap, new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client"))
    val restClient = new rest.Client(config.zuoraRest, new ServiceMetrics(Config.stage, Config.appName, "zuora-rest-client"))

    val digipackConfig = ProductFamilyRatePlanIds.config(Some(Config.config))(config.environmentName, Membership)
    val digipackRatePlanIds = Config.digipackRatePlanIds(config.environmentName)

    val membershipRatePlanIds = Config.membershipRatePlanIds(config.environmentName)
    val catalogService = CatalogService(restClient, membershipRatePlanIds, digipackRatePlanIds, config.environmentName)
    val zuoraService = new zuora.ZuoraService(soapClient, restClient, digipackRatePlanIds)
    val promoService = new PromoService(digipackConfig)
    val _stripeService = new StripeService(config.stripe, new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = config.stripe.envName
      val service = "Stripe"
    })
    val paymentService = new PaymentService {
      override def stripeService = _stripeService
    }

    TouchpointBackend(
      config.environmentName,
      salesforceService,
      catalogService,
      zuoraService,
      paymentService,
      config.zuoraProperties,
      promoService
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

case class TouchpointBackend(environmentName: String,
                             salesforceService: SalesforceService,
                             catalogService : api.CatalogService,
                             zuoraService: zuora.api.ZuoraService,
                             paymentService: PaymentService,
                             zuoraProperties: ZuoraProperties,
                             promoService: PromoService) {

  private val that = this

  private val exactTargetService = new ExactTargetService {
    override def zuoraService = that.zuoraService
  }

  val checkoutService =
    new CheckoutService(IdentityService,
                        salesforceService,
                        paymentService,
                        catalogService,
                        zuoraService,
                        exactTargetService,
                        zuoraProperties,
                        promoService)
}
