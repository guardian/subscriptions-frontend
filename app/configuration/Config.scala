package configuration

import com.github.nscala_time.time.Imports._
import com.gocardless.GoCardlessClient
import com.gocardless.GoCardlessClient.Environment
import com.gu.cas.PrefixedTokens
import com.gu.config._
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.memsub.auth.common.MemSub.Google._
import com.gu.monitoring.{ServiceMetrics}
import com.gu.salesforce.SalesforceConfig
import com.gu.subscriptions.{CASApi, CASService}
import com.gu.okhttp.RequestRunners
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import net.kencochrane.raven.dsn.Dsn
import org.joda.time.Days
import play.api.mvc.{Call, RequestHeader}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Try

object Config {

  val appName = "subscriptions-frontend"
  val config = ConfigFactory.load()

  val playSecret = config.getString("play.crypto.secret")

  lazy val googleAuthConfig = googleAuthConfigFor(config)

  lazy val googleGroupChecker = googleGroupCheckerFor(config)

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"

  val timezone = DateTimeZone.forID("Europe/London")

  object Identity {
    private val idConfig = config.getConfig("identity")

    val baseUri = idConfig.getString("baseUri")
    val apiToken = idConfig.getString("apiToken")

    val keys = if (idConfig.getBoolean("production.keys")) new ProductionKeys else new PreProductionKeys

    val testUsersSecret = idConfig.getString("test.users.secret")

    val webAppUrl = idConfig.getString("webapp.url")

    val webAppProfileUrl = webAppUrl / "account" / "edit"

    val webAppMMAUrl = webAppUrl / "digitalpack" / "edit"

    val sessionDomain = idConfig.getString("sessionDomain")

    def idWebAppSigninUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("signin", returnTo)

    def idWebAppSignOutUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("signout", returnTo)

    def idWebAppRegisterUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("register", returnTo)

    def idWebAppUrl(idPath: String, returnTo : Call)(implicit request: RequestHeader): String =
      (webAppUrl / idPath) ? ("returnUrl" -> returnTo.absoluteURL(secure = true)) ? ("skipConfirmation" -> "true")

  }

  object Zuora {
    private val stageConfig = config.getConfig("touchpoint.backend.environments").getConfig(stage)

    val paymentDelay = Days.days(stageConfig.getInt("zuora.paymentDelayInDays"))
  }

  val subscriptionsUrl = config.getString("subscriptions.url")

  val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

  lazy val Salesforce =  SalesforceConfig.from(config.getConfig("touchpoint.backend.environments").getConfig(stage), stage)

  object GoCardless {
    private val token = config.getString("gocardless.token")
    val client = GoCardlessClient.create(token, Environment.SANDBOX)
  }

  def discountRatePlanIds(env: String): DiscountRatePlanIds =
    DiscountRatePlanIds.fromConfig(config.getConfig(s"touchpoint.backend.environments.$env.zuora.ratePlanIds"))

  def holidayRatePlanIds(env: String) =
    HolidayRatePlanIds(config.getConfig(s"touchpoint.backend.environments.$env.zuora.ratePlanIds.discount.deliverycredit"))

  def productIds(env: String): com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds =
    SubsV2ProductIds(config.getConfig(s"touchpoint.backend.environments.$env.zuora.productIds"))

  object CAS {
    lazy val casConf = config.getConfig("cas")
    lazy val url = casConf.getString("url")
    lazy val emergencyEncoder = {
      val conf = casConf.getConfig("emergency.subscriber.auth")
      val prefix = conf.getString("prefix")
      val secret = conf.getString("secret")
      PrefixedTokens(secretKey = secret, emergencySubscriberAuthPrefix = prefix)
    }
  }

  lazy val casService = {
    val metrics = new ServiceMetrics(stage, appName, "CAS service")
    val api = new CASApi(CAS.url, RequestRunners.loggingRunner(metrics))
    new CASService(api)
  }

  val previewXFrameOptionsOverride = config.getString("subscriptions.preview-x-frame-options-override")

  val welcomeEmailQueue = config.getString("aws.queue.welcome-email")

  val holidaySuspensionEmailQueue = config.getString("aws.queue.holiday-suspension-email")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val suspendableWeeks = 6

  val getAddressIOApiUrl = config.getString("get-address-io-api.url")
  val getAddressIOApiKey = config.getString("get-address-io-api.key")

}
