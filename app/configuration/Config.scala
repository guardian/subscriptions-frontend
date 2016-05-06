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
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import monitoring.Metrics
import net.kencochrane.raven.dsn.Dsn
import org.joda.time.Days
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

  object ExactTarget {
    val clientId = config.getString("exact-target.client-id")
    val clientSecret = config.getString("exact-target.client-secret")
    val welcomeTriggeredSendKey = config.getString("exact-target.triggered-send-keys.welcome")
  }

  def digipackRatePlanIds(env: String): DigitalPackRatePlanIds =
    DigitalPackRatePlanIds.fromConfig(ProductFamilyRatePlanIds.config(Some(config))(env, Digipack))

  def discountRatePlanIds(env: String): DiscountRatePlanIds =
    DiscountRatePlanIds.fromConfig(config.getConfig(s"touchpoint.backend.environments.$env.zuora.ratePlanIds"))

  def membershipRatePlanIds(env: String) =
    MembershipRatePlanIds.fromConfig(ProductFamilyRatePlanIds.config(Some(config))(env, Membership))

  def getPromotions(env: String) : Seq[AnyPromotion] = {
    val webChannel = Channel("Web")
    val emailChannel = Channel("Email")
    val prpIds = digipackRatePlanIds(env)
    val appliesToUKOnly = AppliesTo.ukOnly(prpIds.productRatePlanIds)
    val appliesToEveryone = AppliesTo.all(prpIds.productRatePlanIds)
    Seq({
      val jellyFishPromoCode = PromoCode("DGA85")
      Promotion(
        name = "Free £30 digital gift card when you subscribe",
        description = "Get £30 to spend with a top retailer of your choice when you subscribe. Use your digital gift card at Amazon.co.uk, M&S and more. Treat yourself or a friend.",
        appliesTo = appliesToUKOnly,
        campaign = Campaign(code = "", name = "DigiPack - £30 digital gift card"),
        channelCodes = Map(
          webChannel -> ((88 to 94).map(i => PromoCode(s"DGA$i")) ++ Seq(jellyFishPromoCode)).toSet,
          emailChannel -> (88 to 94).map(i => PromoCode(s"DGB$i")).toSet
        ),
        landingPage = Some(LandingPage(
          title = None,
          description = None,
          roundelHtml = Some("<span class='roundel__strong'>£30</span> digital gift card"),
          imageUrl = Some(Uri.parse("https://media.guim.co.uk/b26ecf643d6494d60fc32c94e43d8d1483daadac/0_0_720_418/720.jpg"))
        )),
        starts = new LocalDate(2016, 3, 1).toDateTime(LocalTime.Midnight, timezone),
        expires = Some(new LocalDate(2016, 6, 1).toDateTime(LocalTime.Midnight, timezone)),
        promotionType = Incentive(
          redemptionInstructions = "We'll send redemption instructions to your registered email address",
          termsAndConditions = "<h4>Giftcloud £30 gift card terms and conditions</h4><p>Offer only available to customers who subscribe after trial period. Customers are asked to allow up to 35 days from first payment date to receive their gift card redemption email. Offer available to customers who subscribe after trial period only. Once customers have received their gift card redemption email they will have 90 days to claim the £30 gift card by selecting their chosen digital gift card and entering their email or phone number after which time the gift will no longer be available. In the event stock runs out you may be offered an alternative gift of a similar value or a full refund. GNM reserves the right to withdraw this promotion at any time.</p> <p>Amazon.co.uk Gift Cards (“GCs”) sold by Giftcloud, an authorised and independent reseller of Amazon.co.uk Gift Cards. Amazon.co.uk Gift Cards may be redeemed on the Amazon.co.uk website towards the purchase of eligible products listed in our online catalogue and sold by Amazon.co.uk or any other seller selling through Amazon.co.uk. GCs cannot be reloaded, resold, transferred for value, redeemed for cash or applied to any other account. Amazon.co.uk is not responsible if a GC is lost, stolen, destroyed or used without permission. See <a class='u-link' href='http://www.amazon.co.uk/gc-legal' target='_blank'>www.amazon.co.uk/gc-legal</a> for complete terms and conditions. GCs are issued by Amazon EU S.à r.l. All Amazon ®, ™ & © are IP of Amazon.com, Inc. or its affiliates.</p>"
        )
      )
    }, {
      Promotion(
        name = "More of the Guardian, for less",
        description = "For a limited time you can enjoy the digital pack for a special discounted price. Get every issue of The Guardian and The Observer newspapers delivered to your tablet, plus an ad-free experience on The Guardian live news app.",
        appliesTo = appliesToEveryone,
        campaign = Campaign(code = "", name = "DigiPack for just £9.99 a month (~17% discount)"),
        channelCodes = Map(
          webChannel -> (13 to 26).map(i => PromoCode(s"DPA$i")).toSet,
          emailChannel -> (13 to 26).map(i => PromoCode(s"DPB$i")).toSet
        ),
        landingPage = Some(LandingPage(
          title = None,
          description = None,
          roundelHtml = Some("Only £9.99 a month</span><span class='roundel__byline'>usually £11.99"),
          imageUrl = Some(Uri.parse("https://media.guim.co.uk/b26ecf643d6494d60fc32c94e43d8d1483daadac/0_0_720_418/720.jpg"))
        )),
        starts = new LocalDate(2016, 3, 1).toDateTime(LocalTime.Midnight, timezone),
        expires = None,
        promotionType = PercentDiscount(None, 16.680567139283)
      )
    }, {
      Promotion(
        name = "Try the Guardian DigiPack free for 30 Days",
        description = "Enjoy the digital pack for free for 30 days without charge. Get every issue of The Guardian and The Observer newspapers delivered to your tablet, plus an ad-free experience on The Guardian live news app.",
        appliesTo = appliesToEveryone,
        campaign = Campaign(code = "", name = "DigiPack free for 30 Days"),
        channelCodes = Map(webChannel -> (22 to 25).map(i => PromoCode(s"DHA$i")).toSet),
        landingPage = Some(LandingPage(
          title = None,
          description = None,
          roundelHtml = Some("<span class='roundel__strong'>FREE</span> DigiPack for 30 days"),
          imageUrl = None
        )),
        starts = new LocalDate(2016, 4, 1).toDateTime(LocalTime.Midnight, timezone),
        expires = Some(new LocalDate(2016, 6, 30).toDateTime(LocalTime.Midnight, timezone)),
        promotionType = FreeTrial(duration = Days.days(30))
      )
    }, {
      Promotion(
        name = "Student price £7.49 per month",
        description = "Student offer - £7.49 per month",
        appliesTo = appliesToUKOnly,
        campaign = Campaign(code = "", name = "Student offer codes"),
        channelCodes = Map(
          webChannel -> (1 to 15).map(i => PromoCode(f"DSA$i%02d")).toSet,
          emailChannel -> (1 to 15).map(i => PromoCode(f"DSB$i%02d")).toSet
        ),
        landingPage = Some(LandingPage(
          title = None,
          description = None,
          roundelHtml = Some("Student price £7.49 per month"),
          imageUrl = None
        )),
        starts = new LocalDate(2016, 5, 5).toDateTime(LocalTime.Midnight, timezone),
        expires = None,
        promotionType = PercentDiscount(durationMonths = None, amount = 37.531276063386)
      )
    }, {
      Promotion(
        name = "Enjoy a free coffee world pack from Pact",
        description = "Let the paper and the coffee come to you. Get a free world coffee pack from Pact when you subscribe. Enjoy a selection of 3 80g bags of coffee from around the world delivered to your door. So you can sit back and enjoy your morning read without having to leave the house.\nPact deliver coffees from around the world through your letter box, ground and packed within 7 days of roasting for optimal freshness. Their coffees are sourced through Direct Trade so Pact can get the best quality beans and pay farmers a better price.",
        appliesTo = appliesToUKOnly,
        campaign = Campaign(code = "", name = "Pact coffee campaign"),
        channelCodes = Map(
          webChannel -> (95 to 99).map(i => PromoCode(s"DGA$i")).toSet
        ),
        landingPage = Some(LandingPage(
          title = None,
          description = None,
          roundelHtml = Some("Free <span class='roundel__strong'>PACT</span> Coffee!"),
          imageUrl = None
        )),
        starts = new LocalDate(2016, 5, 2).toDateTime(LocalTime.Midnight, timezone),
        expires = Some(new LocalDate(2016, 6, 1).toDateTime(LocalTime.Midnight, timezone)),
        promotionType = Incentive(
          redemptionInstructions = "We'll send redemption instructions to your registered email address. Redemption of Pact coffee world pack will require customers to create an account with Pact coffee to enable delivery.",
          termsAndConditions = "<h4>Pact Coffee world pack terms and conditions</h4><p>Offer available in the UK only. Offer of free Pact coffee world pack available to new digital pack subscribers only. Redemption of Pact coffee world pack will require customers to create an account with Pact coffee to enable delivery.</p>"
        )
      )
    })
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
