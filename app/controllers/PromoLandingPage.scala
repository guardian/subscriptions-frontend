package controllers
import actions.CommonActions._
import com.gu.memsub.promo._
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion._
import com.netaporter.uri.dsl._
import configuration.Config
import filters.HandleXFrameOptionsOverrideHeader
import model.DigitalEdition

import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import play.api.mvc._
import services.TouchpointBackend
import views.support.PegdownMarkdownRenderer
import utils.Tracking.internalCampaignCode

object PromoLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  val catalog = tpBackend.catalogService.unsafeCatalog
  val edition = DigitalEdition.UK

  def render(promoCodeStr: String) = CachedAction { implicit request =>
    val evaluateStarts = tpBackend.environmentName == "PROD"
    val promoCode = PromoCode(promoCodeStr)

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      promotionWithLandingPage <- promotion.asDigipack
      html <- if ((evaluateStarts && promotionWithLandingPage.starts.isAfterNow) || promotionWithLandingPage.expires.exists(_.isBeforeNow))
        None
      else
        Some(views.html.promotion.landingPage(edition, catalog.digipack.month, promoCode, promotionWithLandingPage, Config.Zuora.paymentDelay, PegdownMarkdownRenderer))
    } yield Ok(html)).getOrElse(Redirect("/" ? (internalCampaignCode -> s"FROM_P_${promoCode.get}")))
  }

  def preview() = GoogleAuthenticatedStaffAction { implicit request =>
    Form(Forms.single("promoJson" -> Forms.text)).bindFromRequest().fold(_ => NotFound, { jsString =>
      Json.fromJson[AnyPromotion](Json.parse(jsString)).asOpt.flatMap(_.asDigipack)
      .map(p => views.html.promotion.landingPage(edition, catalog.digipack.month, p.codes.headOption.getOrElse(PromoCode("")), p, Config.Zuora.paymentDelay, PegdownMarkdownRenderer))
      .fold[Result](NotFound)(p => Ok(p).withHeaders(HandleXFrameOptionsOverrideHeader.HEADER_KEY -> s"ALLOW-FROM ${Config.previewXFrameOptionsOverride}"))
    })
  }
}
