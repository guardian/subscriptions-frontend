package controllers

import actions.CommonActions._
import com.gu.memsub.promo.{Incentive, PromoCode, Promotion}
import model.DigitalEdition
import play.api.mvc._
import services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie

object PromoLandingPage extends Controller {

  def loadTemplateForPromotion(promoCode: PromoCode, promotion: Promotion, backend: TouchpointBackend) =
    promotion.promotionType match {
      case Incentive =>
        val catalog = backend.catalogService.digipackCatalog
        val edition = DigitalEdition.UK
        Some(views.html.promotion.landingPage_incentive(edition, catalog, promoCode, promotion))
      case _ => None
    }

  def render(promoCodeStr: String) = GoogleAuthenticatedStaffAction { implicit request =>
    val promoCode = PromoCode(promoCodeStr)
    val tpBackend = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies).backend

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      html <- if (promotion.expires.isBeforeNow) None else loadTemplateForPromotion(promoCode, promotion, tpBackend)
    } yield Ok(html)).getOrElse(NotFound(views.html.error404()))

  }
}
