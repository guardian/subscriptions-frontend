package controllers

import actions.CommonActions._
import com.gu.memsub.promo._
import com.netaporter.uri.dsl._
import configuration.Config
import model.DigitalEdition
import play.api.mvc._
import services.TouchpointBackend

object PromoLandingPage extends Controller {

  def render(promoCodeStr: String) = CachedAction { implicit request =>
    val promoCode = PromoCode(promoCodeStr)
    val tpBackend = TouchpointBackend.Normal
    val catalog = tpBackend.catalogService.digipackCatalog
    val edition = DigitalEdition.UK
    val evaluateStarts = tpBackend.environmentName == "PROD"

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      promotionWithLandingPage <- Promotion.withLandingPage(promotion)
      html <- if ((evaluateStarts && promotionWithLandingPage.starts.isAfterNow) || promotionWithLandingPage.expires.exists(_.isBeforeNow)) None else Some(views.html.promotion.landingPage(edition, catalog, promoCode, promotionWithLandingPage, Config.Zuora.paymentDelay))
    } yield Ok(html)).getOrElse(Redirect("/digital" ? ("INTCMP" -> s"FROM_P_${promoCode.get}")))
  }
}
