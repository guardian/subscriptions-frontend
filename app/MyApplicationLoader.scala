import actions.{CommonActions, OAuthActions}
import com.gu.googleauth.GoogleAuthConfig
import com.gu.memsub.auth.common.MemSub.Google.googleAuthConfigFor
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.Config
import configuration.Config.config
import controllers._
import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import loghandling.Logstash
import monitoring.{ErrorHandler, SentryLogging}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.components.HttpConfigurationComponents
import play.filters.csrf.CSRFComponents
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}
import router.Routes
import services.{AuthenticationService, TouchpointBackends}
import utils.TestUsers

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    try {
      SentryLogging.init()
      Logstash.init(Config)
      new MyComponents(context).application
    } catch {
      case e: Throwable =>
        SafeLogger.error(scrub"Failed to load application due to $e")
        throw e
    }
  }
}

class MyComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with CSRFComponents
    with HttpConfigurationComponents
    with AssetsComponents
{

  val authenticationService: AuthenticationService =
    AuthenticationService.unsafeInit(Config.Identity.baseUri, Config.Identity.apiToken)

  val testUsers = new TestUsers(authenticationService)

  val sessionSubscription = new SessionSubscription(authenticationService)

  val touchpointBackends = new TouchpointBackends(testUsers, actorSystem, wsClient, executionContext)

  override lazy val httpErrorHandler: ErrorHandler =
    new ErrorHandler(environment, configuration, sourceMapper, Some(router))

  val commonActions = new CommonActions(executionContext = executionContext, csrfCheck, parser = playBodyParsers.default)
  val oAuthActions = new OAuthActions(wsClient, commonActions, playBodyParsers.default, googleAuthConfig)

  lazy val googleAuthConfig: GoogleAuthConfig = googleAuthConfigFor(config, httpConfiguration = httpConfiguration)

  val httpClient = com.gu.okhttp.RequestRunners.futureRunner

  lazy val router: Routes = new Routes(
    httpErrorHandler,
    new CachedAssets(assets, controllerComponents),
    new Homepage(commonActions, controllerComponents),
    new Management(actorSystem = actorSystem, touchpointBackends, oAuthActions, executionContext, controllerComponents),
    new DigitalPack(touchpointBackends.Normal, commonActions, controllerComponents),
    new CheckoutHandler(touchpointBackends, authenticationService, testUsers, commonActions, executionContext, controllerComponents),
    new Checkout(touchpointBackends, authenticationService, commonActions, executionContext, controllerComponents),
    new Promotion(touchpointBackends, commonActions, executionContext, controllerComponents),
    new Shipping(touchpointBackends.Normal, commonActions, controllerComponents),
    new WeeklyLandingPage(touchpointBackends.Normal, commonActions, controllerComponents),
    new OAuth(wsClient = wsClient, commonActions, oAuthActions, executionContext, controllerComponents),
    new CAS(oAuthActions, executionContext, controllerComponents),
    new AccountManagement(touchpointBackends, authenticationService, sessionSubscription, commonActions, httpClient, controllerComponents),
    new PatternLibrary(commonActions, controllerComponents),
    new GeoCountry(commonActions, controllerComponents),
    new Testing(touchpointBackends.Test, commonActions, oAuthActions, controllerComponents),
    new PromoLandingPage(touchpointBackends.Normal, commonActions, oAuthActions, executionContext, controllerComponents),
    new Offers(commonActions, controllerComponents)
  )

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    new HandleXFrameOptionsOverrideHeader(),
    new CheckCacheHeadersFilter(),
    new SecurityHeadersFilter(SecurityHeadersConfig.fromConfiguration(context.initialConfiguration)),
    csrfFilter,
    new AddGuIdentityHeaders(authenticationService),
    new AddEC2InstanceHeader(wsClient)
  )

}
