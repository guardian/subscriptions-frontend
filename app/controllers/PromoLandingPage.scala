package controllers
import javax.inject.Inject

import actions.{CommonActions, OAuthActions}
import com.gu.i18n.CountryGroup.byCountryCode
import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.Benefit.Digipack
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.netaporter.uri.dsl._
import configuration.Config
import controllers.SessionKeys.PromotionTrackingCode
import controllers.WeeklyLandingPage.{Hreflang, Hreflangs}
import filters.HandleXFrameOptionsOverrideHeader
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.TouchpointBackend
import utils.RequestCountry._
import utils.Tracking.internalCampaignCode
import views.html.promotion._
import views.html.weekly.landing_description
import views.support.PegdownMarkdownRenderer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient

class PromoLandingPage @Inject()(override val wsClient: WSClient) extends Controller with OAuthActions {

  private val tpBackend = TouchpointBackend.Normal
  private val catalog = tpBackend.catalogService.unsafeCatalog

  private val digitalPackRatePlanIds = catalog.digipack.plans.map(_.id).toSet
  private val allPaperPackages = catalog.delivery.list ++ catalog.voucher.list
  private val paperPlusPackageRatePlanIds = allPaperPackages.filter(_.charges.benefits.list.contains(Digipack)).map(_.id).toSet
  private val paperOnlyPackageRatePlanIds = allPaperPackages.filterNot(_.charges.benefits.list.contains(Digipack)).map(_.id).toSet
  private val guardianWeeklyRatePlanIds = catalog.weekly.zoneA.plans.map(_.id).toSet ++ catalog.weekly.zoneC.plans.map(_.id).toSet

  private def isActive(promotion: AnyPromotion): Boolean = {
    val isTest = tpBackend.environmentName != "PROD"
    isTest || (
      promotion.starts.isBeforeNow &&
      !promotion.expires.exists(_.isBeforeNow) &&
      promotion.appliesTo.productRatePlanIds.nonEmpty &&
      promotion.appliesTo.countries.nonEmpty
    )
  }

  private def getDigitalPackLandingPage(promotion: AnyPromotion, country: Country, hreflangs: Hreflangs)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asDigitalPack.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      val currency = byCountryCode(country.alpha2).getOrElse(CountryGroup.UK).currency
      digitalpackLandingPage(currency, country, catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer, hreflangs)
    }
  }

  private def getNewspaperLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asNewspaper.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      newspaperLandingPage(catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getWeeklyLandingPage(promotion: AnyPromotion, country: Country, hreflangs: Hreflangs)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asWeekly.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      val description = promotionWithLandingPage.landingPage.description.map{desc =>
        Html(PegdownMarkdownRenderer.render(desc)
      )}.getOrElse(landing_description())
      weeklyLandingPage(country, catalog, Some(promoCode), Some(promotionWithLandingPage), description, PegdownMarkdownRenderer, hreflangs)
    }
  }

  private def getLandingPage(promotion: AnyPromotion, country: Country, hreflangs: Hreflangs)(implicit promoCode: PromoCode): Option[Html] = {
    getNewspaperLandingPage(promotion) orElse getDigitalPackLandingPage(promotion, country, hreflangs) orElse getWeeklyLandingPage(promotion, country, hreflangs)
  }

  private def getBrochureRouteForPromotion(promotion: AnyPromotion, country: Country): Option[Call] = {
    val applicableRatePlanIds = promotion.appliesTo.productRatePlanIds
    if ((applicableRatePlanIds intersect digitalPackRatePlanIds).nonEmpty) {
      Some(routes.DigitalPack.redirect())
    } else if ((applicableRatePlanIds intersect paperPlusPackageRatePlanIds).nonEmpty) {
      Some(routes.Shipping.viewCollectionPaperDigital())
    } else if ((applicableRatePlanIds intersect paperOnlyPackageRatePlanIds).nonEmpty) {
      Some(routes.Shipping.viewCollectionPaper())
    } else if ((applicableRatePlanIds intersect guardianWeeklyRatePlanIds).nonEmpty) {
      Some(routes.WeeklyLandingPage.withCountry(country.alpha2))
    } else {
      None
    }
  }

  private def redirectToBrochurePage(promotion: AnyPromotion, country: Country)(implicit promoCode: PromoCode, request: Request[AnyContent]): Option[Result] = {
    getBrochureRouteForPromotion(promotion, country) map { route =>
      val result = Redirect(route.url, request.queryString - "country")
      if (promotion.promotionType == Tracking) {
        result.withSession(request.session.data.toSeq ++ Seq(PromotionTrackingCode -> promoCode.get) :_*)
      } else {
        result
      }
    }
  }

  def render(promoCodeStr: String, maybeCountry: Option[Country]): Action[AnyContent] = NoCacheAction.async { implicit request =>
    implicit val promoCode = PromoCode(promoCodeStr)

    tpBackend.promoService.findPromotionFuture(promoCode) map { maybePromotion =>
      maybePromotion flatMap { promotion =>
        val country = (maybeCountry orElse request.getFastlyCountry).getOrElse(Country.UK)

        val hreflangs = CountryGroup.countries.map { country =>
          Hreflang(Config.subscriptionsUrl + routes.PromoLandingPage.render(promoCodeStr, Some(country)).url, s"en-${country.alpha2}")
        }
        val hreflang = Hreflangs(Config.subscriptionsUrl + routes.PromoLandingPage.render(promoCodeStr, Some(country)).url, hreflangs)

        getLandingPage(promotion, country, hreflang).map(Ok(_)) orElse redirectToBrochurePage(promotion, country)
      } getOrElse {
        Redirect(routes.Homepage.index().url ? (internalCampaignCode -> s"FROM_P_${promoCode.get}"), request.queryString)
      }
    }
  }

  def preview(maybeCountry: Option[Country]) = GoogleAuthenticatedStaffAction { implicit request =>
    //User can preview a promotion before assigning it a code.
    val undefinedPromoCode = PromoCode("PromoCode")
    //This appears in a frame in the promoTool.
    def OkWithPreviewHeaders(html: Html) = Ok(html).withHeaders(HandleXFrameOptionsOverrideHeader.HEADER_KEY -> s"ALLOW-FROM ${Config.previewXFrameOptionsOverride}")

    val formSerializedPromotion = Form(Forms.single("promoJson" -> Forms.text)).bindFromRequest()
    val maybePromotion = formSerializedPromotion.fold(_ => None, { jsString =>
      Json.fromJson[AnyPromotion](Json.parse(jsString)).asOpt
    })

    val maybeLandingPage = maybePromotion.flatMap { promotion =>
      val promoCode: PromoCode = promotion.codes.headOption.getOrElse(undefinedPromoCode)
      val country = (maybeCountry orElse request.getFastlyCountry).getOrElse(Country.UK)

      val hreflangs = CountryGroup.countries.map { country =>
        Hreflang(Config.subscriptionsUrl + routes.PromoLandingPage.preview(Some(country)).url, s"en-${country.alpha2}")
      }
      val hreflang = Hreflangs(Config.subscriptionsUrl + routes.PromoLandingPage.preview(Some(country)).url, hreflangs)

      getLandingPage(promotion, country, hreflang)(promoCode)
    }

    maybeLandingPage.map(OkWithPreviewHeaders).getOrElse(NotFound)
  }

  def terms(promoCodeStr: String): Action[AnyContent] = CachedAction.async { _ =>
    val promoCode = PromoCode(promoCodeStr)

    tpBackend.promoService.findPromotionFuture(promoCode) map { maybePromotion =>
      maybePromotion.map { promotion =>
        Ok(views.html.promotion.termsPage(promoCode, promotion, PegdownMarkdownRenderer, catalog))
      } getOrElse {
        Redirect(routes.Homepage.index().url ? (internalCampaignCode -> s"FROM_PT_${promoCode.get}"))
      }
    }
  }
}
