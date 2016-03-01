package configuration

import com.github.nscala_time.time.Imports._
import com.gocardless.GoCardlessClient
import com.gocardless.GoCardlessClient.Environment
import com.gu.cas.PrefixedTokens
import com.gu.config.{DigitalPackRatePlanIds, DiscountRatePlanIds, MembershipRatePlanIds, ProductFamilyRatePlanIds}
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.memsub.auth.common.MemSub.Google._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.{Digipack, Membership}
import com.gu.monitoring.StatusMetrics
import com.gu.salesforce.SalesforceConfig
import com.gu.subscriptions.{CASApi, CASService}
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import monitoring.Metrics
import net.kencochrane.raven.dsn.Dsn
import play.api.mvc.{Call, RequestHeader}

import scala.util.Try

object Config {
  val appName = "subscriptions-frontend"
  val config = ConfigFactory.load()

  val playSecret = config.getString("play.crypto.secret")

  lazy val googleAuthConfig = googleAuthConfigFor(config)

  lazy val googleGroupChecker = googleGroupCheckerFor(config)

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"


  val trackerUrl = config.getString("snowplow.url")
  val bcryptSalt = config.getString("activity.tracking.bcrypt.salt")
  val bcryptPepper = config.getString("activity.tracking.bcrypt.pepper")

 object Identity {
    private val idConfig = config.getConfig("identity")

    val baseUri = idConfig.getString("baseUri")
    val apiToken = idConfig.getString("apiToken")

    val keys = if (idConfig.getBoolean("production.keys")) new ProductionKeys else new PreProductionKeys

    val testUsersSecret = idConfig.getString("test.users.secret")

    val webAppUrl = idConfig.getString("webapp.url")

    val webAppProfileUrl = webAppUrl / "account" / "edit"

    val sessionDomain = idConfig.getString("sessionDomain")

    def idWebAppSigninUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("signin", returnTo)

    def idWebAppSignOutUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("signout", returnTo)

    def idWebAppRegisterUrl(returnTo: Call)(implicit request: RequestHeader) = idWebAppUrl("register", returnTo)

    def idWebAppUrl(idPath: String, returnTo : Call)(implicit request: RequestHeader): String =
      (webAppUrl / idPath) ? ("returnUrl" -> returnTo.absoluteURL(secure = true)) ? ("skipConfirmation" -> "true")

  }

  object Zuora {
    private val stageConfig = config.getConfig("touchpoint.backend.environments").getConfig(stage)

    val paymentDelay = stageConfig.getInt("zuora.paymentDelayInDays").days
  }

  val subscriptionsUrl = config.getString("subscriptions.url")

  val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

  lazy val Salesforce =  SalesforceConfig.from(config.getConfig("touchpoint.backend.environments").getConfig(stage), stage)

  object GoCardless {
    private val token = config.getString("gocardless.token")
    val client = GoCardlessClient.create(token, Environment.SANDBOX)
  }

  object ExactTarget {
    val clientId = config.getString("exact-target.client-id")
    val clientSecret = config.getString("exact-target.client-secret")
    val welcomeTriggeredSendKey = config.getString("exact-target.triggered-send-keys.welcome")
  }

  def digipackRatePlanIds(env: String): DigitalPackRatePlanIds =
    DigitalPackRatePlanIds.fromConfig(ProductFamilyRatePlanIds.config(Some(config))(env, Digipack))

  def discountRatePlanIds(env: String): DiscountRatePlanIds =
    DiscountRatePlanIds.fromConfig(config.getConfig(s"touchpoint.backend.environments.$env.zuora.ratePlanIds")
  )

  def membershipRatePlanIds(env: String) =
    MembershipRatePlanIds.fromConfig(ProductFamilyRatePlanIds.config(Some(config))(env, Membership))

  def demoPromo(env: String) = {
    val prpIds = digipackRatePlanIds(env)
    Promotion(
      appliesTo = AppliesTo.ukOnly(Set(
        prpIds.digitalPackMonthly,
        prpIds.digitalPackQuaterly,
        prpIds.digitalPackYearly
      )),
      campaignName = "DigiPack - Free £30 digital gift card",
      codes = PromoCodeSet(PromoCode("DGA88"), PromoCode("DGB88")),
      description = "Get £30 to spend with a top retailer of your choice when you subscribe. Use your digital gift card at John Lewis, Amazon, M&S and more. Treat yourself or a friend.",
      expires = new LocalDate(2016,4,1).toDateTime(LocalTime.Midnight, DateTimeZone.forID("Europe/London")),
      imageUrl = Some("https://media.guim.co.uk/076bb31be49a31dfe82869ed2937fc8254917361/0_0_850_418/850.jpg"),
      promotionType = Incentive(
        redemptionInstructions = "We'll send redemption instructions to your registered email address",
        termsAndConditions = "<h4>Giftcloud £30 gift card terms and cond'itions</h4><p>Offer only available to customers who subscribe after trial period. Customers are asked to allow up to 35 days from first payment date to receive their gift card redemption email. Offer available to customers who subscribe after trial period only. Once customers have received their gift card redemption email they will have 90 days to claim the £30 gift card by selecting their chosen digital gift card and entering their email or phone number after which time the gift will no longer be available. In the event stock runs out you may be offered an alternative gift of a similar value or a full refund. GNM reserves the right to withdraw this promotion at any time.</p> <p>Amazon.co.uk Gift Cards (“GCs”) sold by Giftcloud, an authorised and independent reseller of Amazon.co.uk Gift Cards. Amazon.co.uk Gift Cards may be redeemed on the Amazon.co.uk website towards the purchase of eligible products listed in our online catalogue and sold byAmazon.co.uk or any other seller selling through Amazon.co.uk. GCs cannot be reloaded, resold, transferred for value, redeemed for cash or applied to any other account. Amazon.co.uk is not responsible if a GC is lost, stolen, destroyed or used without permission. See <a href='http://www.amazon.co.uk/gc-legal' target='_blank'>www.amazon.co.uk/gc-legal</a> for complete terms and conditions. GCs are issued by Amazon EU S.à r.l. All Amazon ®, ™ & © are IP of Amazon.com, Inc. or its affiliates.</p>"
      ),
      roundelHtml = "Free <span class='roundel__strong'>£30</span> digital gift card",
      thumbnailUrl = "http://lorempixel.com/46/16/abstract",
      title = "Free £30 digital gift card when you subscribe"
    )
  }

  def discountPromo(env: String): Option[AnyPromotion] = {
    val prpIds = digipackRatePlanIds(env)
    Some(Promotion(
      appliesTo = AppliesTo.ukOnly(Set(
        prpIds.digitalPackMonthly,
        prpIds.digitalPackQuaterly,
        prpIds.digitalPackYearly
      )),
      campaignName = s"DigiPack for just £9.99 a month (~17% discount)",
      codes = PromoCodeSet(PromoCode("DPA30")),
      description = "For a limited time you can enjoy the digital pack for just £9.99 a month (usually £11.99). Get every paper delivered to your tablet for less than 35p an edition, plus an ad-free experience on your live news app.",
      expires = new LocalDate(2016,4,1).toDateTime(LocalTime.Midnight, DateTimeZone.forID("Europe/London")),
      imageUrl = None,
      roundelHtml = "Only £9.99 a month</span><span class='roundel__byline'>usually £11.99",
      thumbnailUrl = "http://lorempixel.com/46/16/abstract",
      title = "More of the Guardian, for less",
      promotionType = PercentDiscount(None, 16.680567139283)
    )).filter(_ => env != "PROD")
  }

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
    val metrics = new StatusMetrics with Metrics {
      override val service: String = "CAS service"
    }
    val api = new CASApi(CAS.url, metrics)
    new CASService(api)
  }
}
