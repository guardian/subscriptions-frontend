package controllers

import actions.CommonActions._
import com.gu.memsub.promo.{Free25JohnLewisVoucher, PromoCode, Promotion}
import model.DigitalEdition
import play.api.mvc._
import services.TouchpointBackend
object Promotions extends Controller {

  def findTemplateForPromotion(promoCode: PromoCode, promotion: Promotion) =
    promotion.landingPageTemplate match {
      case Free25JohnLewisVoucher =>
        val catalog = TouchpointBackend.Normal.catalogService.digipackCatalog
        val edition = DigitalEdition.UK
        Some(views.html.promotions.free25JohnLewisVoucher(edition, catalog, promoCode, promotion))
      case _ => None
    }

  def promotionPage(promoCodeStr: String) = GoogleAuthenticatedStaffAction { implicit request =>
    val promoCode = PromoCode(promoCodeStr)

    (for {
      promotion <- TouchpointBackend.Normal.promoService.findPromotion(promoCode)
      html <- if (promotion.expires.isBeforeNow) None else findTemplateForPromotion(promoCode, promotion)
    } yield Ok(html)).getOrElse(NotFound(views.html.error404()))

  }
}
