package services

import akka.actor.ActorSystem
import com.gocardless.GoCardlessClient
import com.gocardless.GoCardlessClient.Environment
import com.gocardless.GoCardlessClient.Environment.{LIVE, SANDBOX}
import com.gu.config.DiscountRatePlanIds
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{DynamoPromoCollection, DynamoTables, PromotionCollection}
import com.gu.memsub.services.{PaymentService => CommonPaymentService, _}
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services.SubscriptionService._
import com.gu.monitoring.ServiceMetrics
import com.gu.okhttp.RequestRunners._
import com.gu.salesforce.SimpleContactRepository
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.subscriptions.suspendresume.SuspensionService
import com.gu.zuora
import com.gu.zuora.rest.SimpleClient
import com.gu.zuora.{ZuoraRestService, rest, soap}
import configuration.Config
import forms.SubscriptionsForm
import monitoring.TouchpointBackendMetrics
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import services.TouchpointBackends.Resolution
import touchpoint.TouchpointBackendConfig.BackendType
import touchpoint.TouchpointBackendConfig.BackendType.Testing
import touchpoint.{TouchpointBackendConfig, ZuoraProperties}
import utils.TestUsers._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}
import scalaz.std.scalaFuture._

trait TouchpointBackend {
  def environmentName: String
  def salesforceService: SalesforceService
  def catalogService : subsv2.services.CatalogService[Future]
  def zuoraService: zuora.api.ZuoraService
  def subscriptionService: subsv2.services.SubscriptionService[Future]
  def stripeUKMembershipService: StripeService
  def stripeAUMembershipService: StripeService
  def paymentService: PaymentService
  def commonPaymentService: Future[CommonPaymentService]
  def zuoraProperties: ZuoraProperties
  def promoService: PromoService
  def promoCollection: PromotionCollection
  def promoStorage: JsonDynamoService[AnyPromotion, Future]
  def discountRatePlanIds: DiscountRatePlanIds
  def suspensionService: SuspensionService[Future]
  def subsForm: Future[SubscriptionsForm]
  def exactTargetService: ExactTargetService
  def checkoutService: CheckoutService
  def goCardlessService: GoCardlessService
  implicit def simpleRestClient: SimpleClient[Future]
}

class TouchpointBackends(actorSystem: ActorSystem, wsClient: WSClient, executionContext: ExecutionContext) {

  def logE[A](a: => A): A = Try(a).recoverWith({case e: Throwable => e.printStackTrace; Failure(e)}).get

  private implicit val system: ActorSystem = actorSystem
  private implicit val executionContextImplicit: ExecutionContext = executionContext

  lazy val identityService = new IdentityService(new IdentityApiClientImpl(wsClient))

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {

    val config = TouchpointBackendConfig.backendType(backendType, Config.config)
    val soapServiceMetrics = new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client")
    val stripeUKMetrics = new ServiceMetrics(Config.stage, Config.appName, "Stripe UK Membership") with TouchpointBackendMetrics {
      val backendEnv = config.stripeUK.envName
    }

    val stripeAUMetrics = new ServiceMetrics(Config.stage, Config.appName, "Stripe AU Membership") with TouchpointBackendMetrics {
      val backendEnv = config.stripeAU.envName
    }
    val soapClient = new soap.ClientWithFeatureSupplier(Set.empty, config.zuoraSoap, loggingRunner(soapServiceMetrics), configurableLoggingRunner(20.seconds, soapServiceMetrics))
    val newProductIds = Config.productIds(config.environmentName)

    lazy val sfSimpleContactRepo = new SimpleContactRepository(config.salesforce, system.scheduler, Config.appName)

    new TouchpointBackend {
      implicit val simpleRestClient: SimpleClient[Future] = new rest.SimpleClient[Future](config.zuoraRest, futureRunner)
      lazy val environmentName: String = config.environmentName
      lazy val salesforceService = new SalesforceServiceImp(sfSimpleContactRepo)
      lazy val catalogService = new subsv2.services.CatalogService[Future](newProductIds, simpleRestClient, Await.result(_, 10.seconds), backendType.name)
      private val slightlyUnsafeCatalog: Future[Catalog] = catalogService.catalog.map(_.valueOr(e => throw new IllegalStateException(s"$e while getting catalog")))
      lazy val zuoraService = new zuora.ZuoraService(soapClient)
      private val map = this.catalogService.catalog.map(_.fold[CatalogMap](error => {println(s"error: ${error.list.toList.mkString}"); Map()}, _.map))
      lazy val subscriptionService = new subsv2.services.SubscriptionService[Future](newProductIds, map, simpleRestClient, zuoraService.getAccountIds)
      lazy val stripeUKMembershipService = new StripeService(config.stripeUK, loggingRunner(stripeUKMetrics))
      lazy val stripeAUMembershipService = new StripeService(config.stripeAU, loggingRunner(stripeAUMetrics))
      lazy val paymentService = new PaymentService(this.stripeUKMembershipService, this.stripeAUMembershipService, this.zuoraProperties.invoiceTemplates.map(it => it.country -> it).toMap)
      lazy val commonPaymentService = slightlyUnsafeCatalog.map(catalog => new CommonPaymentService(zuoraService, catalog.productMap))
      lazy val zuoraProperties: ZuoraProperties = config.zuoraProperties
      lazy val promoService = new PromoService(promoCollection, new Discounter(this.discountRatePlanIds))
      lazy val promoCollection = new DynamoPromoCollection(this.promoStorage, 15.seconds)
      lazy val promoStorage: JsonDynamoService[AnyPromotion, Future] = JsonDynamoService.forTable[AnyPromotion](DynamoTables.promotions(Config.config, config.environmentName))
      lazy val discountRatePlanIds: DiscountRatePlanIds = Config.discountRatePlanIds(config.environmentName)
      lazy val suspensionService = new SuspensionService[Future](Config.holidayRatePlanIds(config.environmentName), simpleRestClient)
      lazy val subsForm = slightlyUnsafeCatalog.map(catalog => new forms.SubscriptionsForm(catalog))
      lazy val exactTargetService: ExactTargetService = new ExactTargetService(this.subscriptionService, this.commonPaymentService, this.zuoraService, this.salesforceService)
      lazy val checkoutService: CheckoutService = new CheckoutService(identityService, this.salesforceService, this.paymentService, slightlyUnsafeCatalog, this.zuoraService, new ZuoraRestService[Future], this.exactTargetService, this.zuoraProperties, this.promoService, this.discountRatePlanIds)
      lazy val goCardlessService: GoCardlessService = new GoCardlessService(config.goCardlessToken)
    }
  }

  lazy val Normal = logE(apply(BackendType.Default))
  lazy val Test = logE(apply(BackendType.Testing))

  /**
   * Alternate credentials are used *only* when the user is not signed in - if you're logged in as
   * a 'normal' non-test user, it doesn't make any difference what pre-signin-test-cookie you have.
   */
  def forRequest[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C)(
    implicit request: RequestHeader): Resolution = {
    val validTestUserCredentialOpt = isTestUser(permittedAltCredentialType, altCredentialSource)
    val backendType = if (validTestUserCredentialOpt.isDefined) BackendType.Testing else BackendType.Default
    Resolution(if (backendType == Testing) Test else Normal, backendType, validTestUserCredentialOpt)
  }
}
object TouchpointBackends {

  case class Resolution(
    backend: TouchpointBackend,
    typ: TouchpointBackendConfig.BackendType,
    validTestUserCredentialOpt: Option[TestUserCredentialType[_]]
  )

}
