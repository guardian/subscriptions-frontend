import actions.{CommonActions, OAuthActions}
import com.gu.googleauth.GoogleAuthConfig
import com.gu.memsub.auth.common.MemSub.Google.googleAuthConfigFor
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
import services.TouchpointBackends

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    SentryLogging.init()
    Logstash.init(Config)
    new MyComponents(context).application
  }
}

class MyComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with CSRFComponents
    with HttpConfigurationComponents
    with AssetsComponents
{

  val touchpointBackends = new TouchpointBackends(actorSystem, wsClient, executionContext)

  override lazy val httpErrorHandler: ErrorHandler =
    new ErrorHandler(environment, configuration, sourceMapper, Some(router))

  val commonActions = new CommonActions(executionContext = executionContext, csrfCheck, parser = playBodyParsers.default)
  val oAuthActions = new OAuthActions(wsClient, commonActions, playBodyParsers.default, googleAuthConfig)

  lazy val googleAuthConfig: GoogleAuthConfig = googleAuthConfigFor(config, httpConfiguration = httpConfiguration)

  val httpClient = com.gu.okhttp.RequestRunners.futureRunner

  lazy val router: Routes = new Routes(
    httpErrorHandler,
    new CachedAssets(assets),
    new Homepage(commonActions),
    new Management(actorSystem = actorSystem, touchpointBackends, oAuthActions),
    new DigitalPack(touchpointBackends.Normal, commonActions),
    new Checkout(touchpointBackends, commonActions),
    new Promotion(touchpointBackends, commonActions),
    new Shipping(touchpointBackends.Normal, commonActions),
    new WeeklyLandingPage(touchpointBackends.Normal, commonActions),
    new OAuth(wsClient = wsClient, commonActions, oAuthActions),
    new CAS(oAuthActions),
    new AccountManagement(touchpointBackends, commonActions, httpClient),
    new PatternLibrary(commonActions),
    new Testing(touchpointBackends.Test, commonActions, oAuthActions),
    new PromoLandingPage(touchpointBackends.Normal, commonActions, oAuthActions),
    new Offers(commonActions)
  )

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    new HandleXFrameOptionsOverrideHeader(),
    new CheckCacheHeadersFilter(),
    new SecurityHeadersFilter(SecurityHeadersConfig.fromConfiguration(context.initialConfiguration)),
    csrfFilter,
    new AddGuIdentityHeaders(),
    new AddEC2InstanceHeader(wsClient)
  )

}
