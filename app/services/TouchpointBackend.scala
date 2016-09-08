package services

import com.gu.config.{DigitalPackRatePlanIds, DiscountRatePlanIds, ProductFamilyRatePlanIds}
import com.gu.memsub.{Digipack, Subscriptions}
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{DynamoPromoCollection, DynamoTables, PromotionCollection}
import com.gu.memsub.services.{PaymentService => CommonPaymentService, _}
import com.gu.monitoring.{ServiceMetrics, StatusMetrics}
import com.gu.salesforce.SimpleContactRepository
import com.gu.stripe.StripeService
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.subscriptions.{DigipackCatalog, Discounter, PaperCatalog}
import com.gu.zuora
import com.gu.zuora.rest.{RequestRunners, SimpleClient}
import com.gu.zuora.{rest, soap}
import configuration.Config
import forms.SubscriptionsForm
import monitoring.TouchpointBackendMetrics
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.RequestHeader
import touchpoint.TouchpointBackendConfig.BackendType
import touchpoint.{TouchpointBackendConfig, ZuoraProperties}
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import scala.concurrent.duration._
import com.gu.zuora.{ZuoraService => ZuoraServiceImpl}


import scalaz.std.scalaFuture._
import utils.TestUsers._

import scala.concurrent.{Await, Future}

object TouchpointBackend {

  private implicit val system = Akka.system

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {
    val config = TouchpointBackendConfig.backendType(backendType, Config.config)
    val salesforceService = new SalesforceServiceImp(new SimpleContactRepository(config.salesforce, system.scheduler, Config.appName))

    val soapClient = new soap.ClientWithFeatureSupplier(Set.empty, config.zuoraSoap, new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client"))
    val restClient = new rest.Client(config.zuoraRest, new ServiceMetrics(Config.stage, Config.appName, "zuora-rest-client"))
    val simpleRestClient = new rest.SimpleClient[Future](config.zuoraRest, RequestRunners.futureRunner)

    val digipackConfig = ProductFamilyRatePlanIds.config(Some(Config.config))(config.environmentName, Subscriptions)
    val digipackRatePlanIds = Config.digipackRatePlanIds(config.environmentName)
    val discountPlans = Config.discountRatePlanIds(config.environmentName)

    val zuoraService = new zuora.ZuoraService(soapClient, restClient)

    val newProductIds = Config.productIds(Config.stage)

    val discounter = new Discounter(discountPlans)
    val membershipRatePlanIds = Config.membershipRatePlanIds(config.environmentName)
    val paperProductIds = Config.paperProductIds(config.environmentName)
    //val catalogService = CatalogService(restClient, paperProductIds, membershipRatePlanIds, digipackRatePlanIds, config.environmentName)

    val promoStorage = JsonDynamoService.forTable[AnyPromotion](DynamoTables.promotions(Config.config, config.environmentName))
    val promoCollection = new DynamoPromoCollection(promoStorage)

    val newCatalogService = new subsv2.services.CatalogService[Future](newProductIds, simpleRestClient, Await.result(_, 10.seconds))
    val newSubsService = new subsv2.services.SubscriptionService[Future](newProductIds, newCatalogService.catalog.map(_.map(_.map)), simpleRestClient, zuoraService.getAccountIds)

    val promoService = new PromoService(promoCollection, discounter)

    val _stripeService = new StripeService(config.stripe, new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = config.stripe.envName
      val service = "Stripe"
    })
    val memsubPaymentService = new CommonPaymentService(_stripeService, zuoraService, newCatalogService.unsafeCatalog.productMap)

    val suspService = new SuspensionService[Future](
      Config.holidayRatePlanIds(config.environmentName),
      new SimpleClient[Future](config.zuoraRest, RequestRunners.futureRunner)
    )

    val form = new forms.SubscriptionsForm(catalogService)


    val paymentService = new PaymentService {
      override def stripeService = _stripeService
    }

    TouchpointBackend(
      config.environmentName,
      salesforceService,
      catalogService,
      zuoraService,
      subService,
      subServicePaper,
      restClient,
      digipackRatePlanIds,
      paymentService,
      memsubPaymentService,
      config.zuoraProperties,
      promoService,
      promoCollection,
      promoStorage,
      discountPlans,
      suspService,
      subsForm = form
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
                             catalogService : subsv2.services.CatalogService[Future],
                             zuoraService: zuora.api.ZuoraService,
                             subscriptionService: subsv2.services.SubscriptionService[Future],
                             zuoraRestClient: zuora.rest.Client,
                             digipackIds: DigitalPackRatePlanIds,
                             paymentService: PaymentService,
                             commonPaymentService: CommonPaymentService,
                             zuoraProperties: ZuoraProperties,
                             promoService: PromoService,
                             promos: PromotionCollection,
                             promoStorage: JsonDynamoService[AnyPromotion, Future],
                             discountRatePlanIds: DiscountRatePlanIds,
                             suspensionService: SuspensionService[Future],
                             subsForm: SubscriptionsForm) {

  private val that = this

  val exactTargetService = new ExactTargetService {
    override def subscriptionService = that.subscriptionService
    override def paymentService = that.commonPaymentService
    override def zuoraService = that.zuoraService
    override def salesforceService = that.salesforceService
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
