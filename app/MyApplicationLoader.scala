import configuration.Config
import controllers._
import filters.{AddEC2InstanceHeader, AddGuIdentityHeaders, CheckCacheHeadersFilter, HandleXFrameOptionsOverrideHeader}
import loghandling.Logstash
import monitoring.SentryLogging
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

  lazy val router = new Routes(
    httpErrorHandler,
    new CachedAssets(),
    new Homepage(),
    new Management(wsClient = wsClient, actorSystem = actorSystem, touchpointBackends),
    new DigitalPack(touchpointBackends.Normal),
    new Checkout(touchpointBackends),
    new Promotion(touchpointBackends),
    new Shipping(touchpointBackends.Normal),
    new WeeklyLandingPage(touchpointBackends.Normal),
    new OAuth(wsClient = wsClient),
    new CAS(wsClient = wsClient),
    new AccountManagement(touchpointBackends),
    new PatternLibrary(),
    new Testing(wsClient = wsClient, touchpointBackends.Test),
    new PromoLandingPage(wsClient = wsClient, touchpointBackends.Normal),
    new Offers()
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
