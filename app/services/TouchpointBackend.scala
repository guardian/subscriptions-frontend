package services

import com.gu.config.{DigitalPackRatePlanIds, DiscountRatePlanIds, ProductFamilyRatePlanIds}
import com.gu.memsub.Digipack
import com.gu.memsub.services.{CatalogService, PromoService, api}
import com.gu.monitoring.{ServiceMetrics, StatusMetrics}
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.zuora
import com.gu.zuora.{rest, soap}
import configuration.Config
import configuration.Config._
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

    val digipackConfig = ProductFamilyRatePlanIds.config(Some(Config.config))(config.environmentName, Digipack)
    val digipackRatePlanIds = Config.digipackRatePlanIds(config.environmentName)
    val discountPlans = Config.discountRatePlanIds(config.environmentName)

    val discounter = new Discounter(discountPlans)
    val membershipRatePlanIds = Config.membershipRatePlanIds(config.environmentName)
    val catalogService = CatalogService(restClient, membershipRatePlanIds, digipackRatePlanIds, config.environmentName)
    val promoService = new PromoService(Seq(demoPromo(config.environmentName)) ++ discountPromo(config.environmentName), catalogService.digipackCatalog, discounter)
    val zuoraService = new zuora.ZuoraService(soapClient, restClient, digipackRatePlanIds)
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
      restClient,
      digipackRatePlanIds,
      paymentService,
      config.zuoraProperties,
      promoService,
      discountPlans
    )
  }

  val BackendsByType = BackendType.All.map(typ => typ -> TouchpointBackend(typ)).toMap

  val Normal = BackendsByType(BackendType.Default)
  val Test = BackendsByType(BackendType.Testing)
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
                             zuoraRestClient: zuora.rest.Client,
                             digipackIds: DigitalPackRatePlanIds,
                             paymentService: PaymentService,
                             zuoraProperties: ZuoraProperties,
                             promoService: PromoService,
                             discountRatePlanIds: DiscountRatePlanIds) {

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
                        promoService,
                        discountRatePlanIds)
}
