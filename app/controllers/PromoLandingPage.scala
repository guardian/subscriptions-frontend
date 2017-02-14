package controllers
import actions.CommonActions._
import com.gu.i18n.{Country, CountryGroup}
import com.gu.i18n.Country
import com.gu.memsub.Digipack
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.netaporter.uri.dsl._
import configuration.Config
import controllers.SessionKeys.PromotionTrackingCode
import filters.HandleXFrameOptionsOverrideHeader
import model.DigitalEdition.UK
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.TouchpointBackend
import utils.RequestCountry.RequestWithFastlyCountry
import utils.Tracking.internalCampaignCode
import utils.Tracking._
import views.html.promotion._
import views.support.PegdownMarkdownRenderer

import scala.concurrent.ExecutionContext.Implicits.global

object PromoLandingPage extends Controller {

  private val tpBackend = TouchpointBackend.Normal
  private val catalog = tpBackend.catalogService.unsafeCatalog

  private val digitalPackRatePlanIds = catalog.digipack.toList.map(_.id).toSet
  private val allPaperPackages = catalog.delivery.list ++ catalog.voucher.list
  private val paperPlusPackageRatePlanIds = allPaperPackages.filter(_.charges.benefits.list.contains(Digipack)).map(_.id).toSet
  private val paperOnlyPackageRatePlanIds = allPaperPackages.filterNot(_.charges.benefits.list.contains(Digipack)).map(_.id).toSet
  private val guardianWeeklyRatePlanIds = catalog.weeklyZoneA.toList.map(_.id).toSet ++ catalog.weeklyZoneC.toList.map(_.id).toSet

  private def isActive(promotion: AnyPromotion): Boolean = {
    val isTest = tpBackend.environmentName != "PROD"
    isTest || (
      promotion.starts.isBeforeNow &&
      !promotion.expires.exists(_.isBeforeNow) &&
      promotion.appliesTo.productRatePlanIds.nonEmpty &&
      promotion.appliesTo.countries.nonEmpty
    )
  }

  private def getDigitalPackLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    // The Digital Pack landing page currently only supports GBP, so only render it for UK-applicative promotions
    promotion.asDigitalPack.filter(p => isActive(asAnyPromotion(p))).filter(_.appliesTo.countries.contains(Country.UK)).map { promotionWithLandingPage =>
      digitalpackLandingPage(UK, catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getNewspaperLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asNewspaper.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      newspaperLandingPage(catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getWeeklyLandingPage(promotion: AnyPromotion, maybeCountry: Option[Country] = None)(implicit promoCode: PromoCode): Option[Html] = {
    val country = maybeCountry.getOrElse(Country.UK)
    promotion.asWeekly.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      weeklyLandingPage(country, catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getLandingPage(promotion: AnyPromotion, maybeCountry: Option[Country]= None)(implicit promoCode: PromoCode): Option[Html] = {
    getNewspaperLandingPage(promotion) orElse getDigitalPackLandingPage(promotion) orElse getWeeklyLandingPage(promotion, maybeCountry)
  }

  private def getBrochureRouteForPromotion(promotion: AnyPromotion): Option[Call] = {
    val applicableRatePlanIds = promotion.appliesTo.productRatePlanIds
    if ((applicableRatePlanIds intersect digitalPackRatePlanIds).nonEmpty) {
      Some(routes.DigitalPack.redirect())
    } else if ((applicableRatePlanIds intersect paperPlusPackageRatePlanIds).nonEmpty) {
      Some(routes.Shipping.viewCollectionPaperDigital())
    } else if ((applicableRatePlanIds intersect paperOnlyPackageRatePlanIds).nonEmpty) {
      Some(routes.Shipping.viewCollectionPaper())
    } else if ((applicableRatePlanIds intersect guardianWeeklyRatePlanIds).nonEmpty) {
      Some(routes.WeeklyLandingPage.index())
    } else {
      None
    }
  }

  private def redirectToBrochurePage(promotion: AnyPromotion)(implicit promoCode: PromoCode, request: Request[AnyContent]): Option[Result] = {
    getBrochureRouteForPromotion(promotion) map { route =>
      val result = Redirect(route.url ? (internalCampaignCode -> s"FROM_P_${promoCode.get}"), request.queryString)
      if (promotion.promotionType == Tracking) {
        result.withSession(PromotionTrackingCode -> promoCode.get)
      } else {
        result
      }
    }
  }

  def render(promoCodeStr: String): Action[AnyContent] = NoCacheAction.async { implicit request =>
    implicit val promoCode = PromoCode(promoCodeStr)

    tpBackend.promoService.findPromotionFuture(promoCode) map { maybePromotion =>
      maybePromotion flatMap { promotion =>
        getLandingPage(promotion).map(Ok(_)) orElse redirectToBrochurePage(promotion)
      } getOrElse {
        Redirect(routes.Homepage.index().url ? (internalCampaignCode -> s"FROM_P_${promoCode.get}"), request.queryString)
      }
    }
  }

  def preview() = GoogleAuthenticatedStaffAction { implicit request =>
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
      getLandingPage(promotion)(promoCode)
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
