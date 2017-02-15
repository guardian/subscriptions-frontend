package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup.UK
import model.DigitalEdition
import model.DigitalEdition._
import play.api.mvc._
import services.TouchpointBackend
import utils.RequestCountry._

object DigitalPack extends Controller {

  private val queryParamHint = "edition"

  private def redirectResult(digitalEdition: DigitalEdition)(implicit request: Request[AnyContent]) = {
    val queryString = request.queryString.-(queryParamHint)
    Redirect(routes.DigitalPack.landingPage(digitalEdition.id).url, queryString, SEE_OTHER)
  }

  def redirect = NoCacheAction { implicit request =>
    // Use hint from 'edition' query parameter if present, else use GEO-IP
    request.getQueryString(queryParamHint).flatMap(getById).map(redirectResult) getOrElse {
      val countryGroup = request.getFastlyCountryGroup.getOrElse(UK) // UK fallback for when no GEO-IP (e.g test env)
      val digitalEdition = getForCountryGroup(countryGroup)
      redirectResult(digitalEdition)
    }
  }

  def landingPage(code: String) = CachedAction { _ =>
    val digitalEdition = getById(code).getOrElse(INT)
    val plan = TouchpointBackend.Normal.catalogService.unsafeCatalog.digipack.month
    val price = plan.charges.price.getPrice(digitalEdition.countryGroup.currency).getOrElse(plan.charges.gbpPrice)
    Ok(views.html.digitalpack.info(digitalEdition, price))
  }

}
