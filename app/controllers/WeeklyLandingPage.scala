package controllers

import javax.inject.Inject

import actions.CommonActions
import com.gu.i18n.{Country, CountryGroup}
import configuration.Config
import model.WeeklyRegion
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.{TouchpointBackend, TouchpointBackends}
import views.html.promotion.weeklyLandingPage
import views.html.weekly.landing_description
import views.support.PegdownMarkdownRenderer
import utils.RequestCountry._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object WeeklyLandingPage{
  case class Hreflang(href: String, lang: String)
  case class Hreflangs(canonical: String, hreflangs: List[Hreflang])
}

class WeeklyLandingPage(tpBackend: TouchpointBackend) extends Controller with CommonActions {

  val international = "int"

  val catalog = tpBackend.catalogService.catalog.map(_.valueOr(e => throw new IllegalStateException(s"$e while getting catalog")))

  def index(country: Option[Country]) = NoCacheAction { implicit request =>
    val maybeCountry = country orElse request.getFastlyCountry
    Redirect(routes.WeeklyLandingPage.withCountry(maybeCountry.map(_.alpha2).getOrElse(international)))
  }

  val description = landing_description()

  def withCountry(country: String) = NoCacheAction.async {
    val parsedCountry = if (country == international) Some(Country.UK) else CountryGroup.countryByCode(country)
    parsedCountry.fold {
      Future.successful(MovedPermanently(routes.WeeklyLandingPage.withCountry(international).url))
    } { country =>
      val hreflangs = CountryGroup.countries.map { country =>
        WeeklyLandingPage.Hreflang(Config.subscriptionsUrl + routes.WeeklyLandingPage.withCountry(country.alpha2).url, s"en-${country.alpha2}")
      }
      val hreflang = WeeklyLandingPage.Hreflangs(Config.subscriptionsUrl + routes.WeeklyLandingPage.withCountry(country.alpha2).url, hreflangs)
      catalog.map { catalog =>
        Ok(weeklyLandingPage(country, catalog, None, None, description, PegdownMarkdownRenderer, hreflangs = hreflang))
      }
    }
  }

}
