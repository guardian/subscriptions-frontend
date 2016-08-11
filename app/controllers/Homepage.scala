package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import model.DigitalEdition._
import utils.RequestCountry._
import play.api.mvc._

object Homepage extends Controller {

  def index = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.UK)
    val digitalEdition = getById(countryGroup.id).getOrElse(INT)
    Redirect(routes.Homepage.landingPage(digitalEdition.id).url, request.queryString, SEE_OTHER)
  }

  def landingPage(code: String) = CachedAction {
    getById(code).fold {
      NotFound(views.html.error404())
    } {
      case UK => Ok(views.html.index())
      case digitalEdition => Ok(views.html.index_intl(digitalEdition))
    }
  }
}
