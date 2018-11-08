package configuration


import com.github.nscala_time.time.Imports._
import com.gu.cas.PrefixedTokens
import com.gu.config._
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.memsub.auth.common.MemSub.Google._
import com.gu.salesforce.SalesforceConfig
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import org.joda.time.Days
import play.api.mvc.{Call, RequestHeader}

import scala.util.Try

object Config {

  val appName = "subscriptions-frontend"
  val config = ConfigFactory.load()

  val playSecret = config.getString("play.crypto.secret")

  lazy val googleGroupChecker = googleGroupCheckerFor(config)

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"

  val timezone = DateTimeZone.forID("Europe/London")

  val membersDataApiUrl = config.getString("members-data-api.url")

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

    val defaultDigitalPackFreeTrialPeriod = Days.days(stageConfig.getInt("zuora.paymentDelayInDays"))
  }

  val subscriptionsUrl = config.getString("subscriptions.url")

  val sentryDsn = Try(config.getString("sentry.dsn"))

  object Logstash {
    private val param = Try{config.getConfig("param.logstash")}.toOption
    val stream = Try{param.map(_.getString("stream"))}.toOption.flatten
    val streamRegion = Try{param.map(_.getString("streamRegion"))}.toOption.flatten
    val enabled = Try{config.getBoolean("logstash.enabled")}.toOption.contains(true)
  }

  lazy val Salesforce =  SalesforceConfig.from(config.getConfig("touchpoint.backend.environments").getConfig(stage), stage)

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

  val previewXFrameOptionsOverride = config.getString("subscriptions.preview-x-frame-options-override")

  val welcomeEmailQueue = config.getString("aws.queue.welcome-email")

  val holidaySuspensionEmailQueue = config.getString("aws.queue.holiday-suspension-email")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val analyticsOnInDev = Try(config.getBoolean("analytics.onInDev")).toOption.getOrElse(false)

  val optimizeEnabled = config.getBoolean("google.optimize.enabled")

  val suspendableWeeks = 6

  val getAddressIOApiUrl = config.getString("get-address-io-api.url")
  val getAddressIOApiKey = config.getString("get-address-io-api.key")

  def fulfilmentLookupApiKey(env: String) = config.getString(s"fulfilment-lookup-api.$env.key")
  def fulfilmentLookupApiUrl(env: String) = config.getString(s"fulfilment-lookup-api.$env.url")

  val compareSavingsWithNewsStandPrices = config.getBoolean("compare.subs.savings.with.newsstand")

  object NewsStandPrices {
    private val newsStandWeeklyPrices = config.getConfig("newsstand.prices.perWeek")

    def getWeeklyRRP(subscription: String): Float = {
      val price: Number = try {
        newsStandWeeklyPrices.getNumber(subscription.replace("+", "plus"))
      } catch {
        case e: Exception => 0
      }
      price.floatValue()
    }
  }

}
