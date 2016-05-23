package controllers

import java.util.UUID

import actions.CommonActions._
import com.gu.memsub.promo._
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import play.api.libs.concurrent.Execution.Implicits._
import com.gu.memsub.promo.Formatters.Common._
import scalaz.std.scalaFuture._
import com.netaporter.uri.dsl._
import configuration.Config
import model.DigitalEdition
import play.api.mvc._
import services.TouchpointBackend

import scala.concurrent.Future
import scala.util.Try
import scalaz.OptionT

object PromoLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  val catalog = tpBackend.catalogService.digipackCatalog
  val edition = DigitalEdition.UK

  def render(promoCodeStr: String) = CachedAction { implicit request =>
    val evaluateStarts = tpBackend.environmentName == "PROD"
    val promoCode = PromoCode(promoCodeStr)

    (for {
      promotion <- tpBackend.promoService.findPromotion(promoCode)
      promotionWithLandingPage <- Promotion.withLandingPage(promotion)
      html <- if ((evaluateStarts && promotionWithLandingPage.starts.isAfterNow) || promotionWithLandingPage.expires.exists(_.isBeforeNow)) None else Some(views.html.promotion.landingPage(edition, catalog, promoCode, promotionWithLandingPage, Config.Zuora.paymentDelay))
    } yield Ok(html)).getOrElse(Redirect("/digital" ? ("INTCMP" -> s"FROM_P_${promoCode.get}")))
  }

  def preview(uuid: String) = GoogleAuthenticatedStaffAction.async { implicit request =>
    (for {
      uuid <- OptionT(Future.successful(Try(UUID.fromString(uuid)).toOption))
      promo <- OptionT(TouchpointBackend.Normal.promoStorage.find(uuid).map(_.headOption))
      withPage <- OptionT(Future.successful(Promotion.withLandingPage(promo)))
    } yield views.html.promotion.landingPage(edition, catalog, promo.codes.head, withPage, Config.Zuora.paymentDelay))
      .run.map(_.fold[Result](NotFound)(h => Ok(h).withHeaders("X-Frame-Options-Override" -> s"ALLOW ${Config.previewXFrameOptionsOverride}")))
  }
}
