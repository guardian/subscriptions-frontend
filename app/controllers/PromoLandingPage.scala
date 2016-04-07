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

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      html <- if (promotion.expires.isBeforeNow) None else Some(views.html.promotion.landingPage(edition, catalog, promoCode, promotion, Config.Zuora.paymentDelay))
    } yield Ok(html)).getOrElse(Redirect("/digital" ? ("INTCMP" -> s"FROM_P_${promoCode.get}")))
  }
}
