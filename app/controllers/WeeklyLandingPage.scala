package controllers

import actions.CommonActions.{CachedAction, _}
import com.gu.i18n.{Country, CountryGroup}
import model.WeeklyRegion
import play.api.mvc._
import services.TouchpointBackend
import views.html.promotion.weeklyLandingPage
import views.html.weekly.landing_description
import views.support.PegdownMarkdownRenderer
import utils.RequestCountry._

object WeeklyLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  val international = "int"

  val catalog = tpBackend.catalogService.unsafeCatalog
  def index(country: Option[Country]) = NoCacheAction { implicit request =>
    val maybeCountry = country orElse request.getFastlyCountry
    Redirect(routes.WeeklyLandingPage.withCountry(maybeCountry.map(_.alpha2).getOrElse(international)))
}
  val description = landing_description()
  def withCountry(country: String) = CachedAction{
    val parsedCountry = if(country == international) Some(Country.UK) else CountryGroup.countryByCode(country)
    parsedCountry.fold{
      MovedPermanently(routes.WeeklyLandingPage.withCountry(international).url)
    } {country =>
      Ok(weeklyLandingPage(country, catalog, None, None,description, PegdownMarkdownRenderer))
    }
  }
}
