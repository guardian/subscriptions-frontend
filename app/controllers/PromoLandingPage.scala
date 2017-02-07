package controllers
import actions.CommonActions._
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.netaporter.uri.dsl._
import configuration.Config
import filters.HandleXFrameOptionsOverrideHeader
import model.DigitalEdition
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.TouchpointBackend
import utils.Tracking.internalCampaignCode
import views.support.PegdownMarkdownRenderer

object PromoLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  lazy val catalog = tpBackend.catalogService.unsafeCatalog
  val edition = DigitalEdition.UK

  private def isActive(promotion: AnyPromotion): Boolean = {
    val isTest = tpBackend.environmentName != "PROD"
    isTest || (promotion.starts.isBeforeNow && !promotion.expires.exists(_.isBeforeNow))
  }

  private def getDigitalPackLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {

    promotion.asDigitalPack.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      views.html.promotion.digitalpackLandingPage(edition, catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getNewspaperLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asNewspaper.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      views.html.promotion.newspaperLandingPage(catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  private def getWeeklyLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    promotion.asWeekly.filter(p => isActive(asAnyPromotion(p))).map { promotionWithLandingPage =>
      views.html.promotion.weeklyLandingPage(catalog, promoCode, promotionWithLandingPage, PegdownMarkdownRenderer)
    }
  }

  def render(promoCodeStr: String) = CachedAction { _ =>
    implicit val promoCode = PromoCode(promoCodeStr)
    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      html <- getLandingPage(promotion)
    } yield Ok(html)).getOrElse(Redirect("/" ? (internalCampaignCode -> s"FROM_P_${promoCode.get}")))
  }
  def getLandingPage(promotion: AnyPromotion)(implicit promoCode: PromoCode): Option[Html] = {
    getNewspaperLandingPage(promotion) orElse getDigitalPackLandingPage(promotion) orElse getWeeklyLandingPage(promotion)
  }

  def preview() = GoogleAuthenticatedStaffAction { implicit request =>
    def OkWithPreviewHeaders(html:Html) = Ok(html).withHeaders(HandleXFrameOptionsOverrideHeader.HEADER_KEY -> s"ALLOW-FROM ${Config.previewXFrameOptionsOverride}")

    val promotion = Form(Forms.single("promoJson" -> Forms.text)).bindFromRequest().fold(_ => None, {jsString =>
      Json.fromJson[AnyPromotion](Json.parse(jsString)).asOpt})
    implicit val promoCode: PromoCode = promotion.flatMap(_.codes.headOption).getOrElse(PromoCode("NOTSET"))
    val landingPage = promotion.flatMap(getLandingPage)

    landingPage.map(OkWithPreviewHeaders).getOrElse(NotFound)
  }

  def terms(promoCodeStr: String) = CachedAction { _ =>
    val promoCode = PromoCode(promoCodeStr)
    val result = for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
    } yield {
      Ok(views.html.promotion.termsPage(promoCode, promotion, PegdownMarkdownRenderer, catalog))
    }
    result.getOrElse(Redirect("/" ? (internalCampaignCode -> s"FROM_PT_${promoCode.get}")))
  }
}
