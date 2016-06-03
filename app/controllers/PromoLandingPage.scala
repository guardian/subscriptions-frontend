package controllers

import actions.CommonActions._
import com.gu.memsub.promo._
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion._
import com.netaporter.uri.dsl._
import configuration.Config
import filters.HandleXFrameOptionsOverrideHeader
import model.DigitalEdition
import play.api.libs.json.Json
import play.api.mvc._
import services.TouchpointBackend

object PromoLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  val catalog = tpBackend.catalogService.digipackCatalog
  val edition = DigitalEdition.UK

  def render(promoCodeStr: String) = CachedAction { implicit request =>
    val evaluateStarts = tpBackend.environmentName == "PROD"
    val promoCode = PromoCode(promoCodeStr)

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      promotionWithLandingPage <- promotion.asDigipack
      html <- if ((evaluateStarts && promotionWithLandingPage.starts.isAfterNow) || promotionWithLandingPage.expires.exists(_.isBeforeNow)) None else Some(views.html.promotion.landingPage(edition, catalog, promoCode, promotionWithLandingPage, Config.Zuora.paymentDelay))
    } yield Ok(html)).getOrElse(Redirect("/digital" ? ("INTCMP" -> s"FROM_P_${promoCode.get}")))
  }

  def preview(json: Option[String]) = GoogleAuthenticatedStaffAction { implicit request =>
    json.flatMap(j => Json.fromJson[AnyPromotion](Json.parse(j)).asOpt).flatMap(_.asDigipack)
      .map(p => views.html.promotion.landingPage(edition, catalog, p.codes.headOption.getOrElse(PromoCode("")), p, Config.Zuora.paymentDelay))
      .fold[Result](NotFound)(p => Ok(p).withHeaders(HandleXFrameOptionsOverrideHeader.HEADER_KEY -> s"ALLOW-FROM ${Config.previewXFrameOptionsOverride}"))
  }
}
