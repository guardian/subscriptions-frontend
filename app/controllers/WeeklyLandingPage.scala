package controllers

import javax.inject.Inject

import actions.CommonActions
import com.gu.i18n.{Country, CountryGroup}
import configuration.Config
import model.WeeklyRegion
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.TouchpointBackend
import views.html.promotion.weeklyLandingPage
import views.html.weekly.landing_description
import views.support.PegdownMarkdownRenderer
import utils.RequestCountry._

object WeeklyLandingPage extends Controller with CommonActions {

  val tpBackend = TouchpointBackend.Normal
  val international = "int"

  val catalog = tpBackend.catalogService.unsafeCatalog

  def index(country: Option[Country]) = NoCacheAction { implicit request =>
    val maybeCountry = country orElse request.getFastlyCountry
    Redirect(routes.WeeklyLandingPage.withCountry(maybeCountry.map(_.alpha2).getOrElse(international)))
  }

  val description = landing_description()

  def withCountry(country: String) = NoCacheAction {
    val parsedCountry = if (country == international) Some(Country.UK) else CountryGroup.countryByCode(country)
    parsedCountry.fold {
      MovedPermanently(routes.WeeklyLandingPage.withCountry(international).url)
    } { country =>
      val hreflangs = CountryGroup.countries.map { country =>
        Hreflang(Config.subscriptionsUrl + routes.WeeklyLandingPage.withCountry(country.alpha2).url, s"en-${country.alpha2}")
      }
      val hreflang = Hreflangs(Config.subscriptionsUrl + routes.WeeklyLandingPage.withCountry(country.alpha2).url, hreflangs)
      Ok(weeklyLandingPage(country, catalog, None, None,description, PegdownMarkdownRenderer, hreflangs = hreflang))
    }
  }

  case class Hreflang(href: String, lang: String)
  case class Hreflangs(canonical: String, hreflangs: List[Hreflang])

}
