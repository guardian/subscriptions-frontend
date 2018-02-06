import actions.{CommonActions, OAuthActions}
import configuration.Config
import controllers._
import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import loghandling.Logstash
import monitoring.{ErrorHandler, SentryLogging}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
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
{

  val touchpointBackends = new TouchpointBackends(actorSystem, wsClient)

  override lazy val httpErrorHandler: ErrorHandler =
    new ErrorHandler(environment, configuration, sourceMapper, Some(router))

  val commonActions = new CommonActions(executionContext = executionContext, csrfCheck, parser = playBodyParsers.default)
  val oAuthActions = new OAuthActions(wsClient = wsClient, commonActions)

  lazy val router: Routes = new Routes(
    httpErrorHandler,
    new CachedAssets(),
    new Homepage(commonActions),
    new Management(actorSystem = actorSystem, touchpointBackends, oAuthActions),
    new DigitalPack(touchpointBackends.Normal, commonActions),
    new Checkout(touchpointBackends, commonActions),
    new Promotion(touchpointBackends, commonActions),
    new Shipping(touchpointBackends.Normal, commonActions),
    new WeeklyLandingPage(touchpointBackends.Normal, commonActions),
    new OAuth(wsClient = wsClient, commonActions),
    new CAS(oAuthActions),
    new AccountManagement(touchpointBackends, commonActions),
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
