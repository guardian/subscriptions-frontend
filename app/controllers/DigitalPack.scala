package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup.UK
import model.DigitalEdition
import model.DigitalEdition._
import play.api.mvc._
import services.TouchpointBackend
import utils.RequestCountry._
import views.support.Pricing._

object DigitalPack extends Controller {

  private def redirectResult(digitalEdition: DigitalEdition)(implicit request: Request[AnyContent]) =
    Redirect(routes.DigitalPack.landingPage(digitalEdition.id).url, request.queryString, SEE_OTHER)

  def redirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(UK)     // UK fallback for when no GEO-IP country (test env)
    val digitalEdition = getForCountryGroup(countryGroup)
    redirectResult(digitalEdition)
  }

  def landingPage(code: String) = CachedAction { implicit request =>
    getById(code).fold(redirectResult(INT)) { digitalEdition =>
      val plan = TouchpointBackend.Normal.catalogService.digipackCatalog.digipackMonthly
      val price = plan.pricing.getPrice(digitalEdition.countryGroup.currency).getOrElse(plan.gbpPrice)
      Ok(views.html.digitalpack.info(digitalEdition, price))
    }
  }

}
