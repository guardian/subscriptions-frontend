package controllers

import actions.CommonActions
import com.gu.i18n.{Country, CountryGroup}
import configuration.Config
import play.api.mvc._
import services.TouchpointBackend
import utils.RequestCountry._
import views.html.promotion.weeklyLandingPage
import views.html.weekly.landing_description
import views.support.PegdownMarkdownRenderer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object WeeklyLandingPage{
  case class Hreflang(href: String, lang: String)
  case class Hreflangs(canonical: String, hreflangs: Option[List[Hreflang]] = None)
}

class WeeklyLandingPage(tpBackend: TouchpointBackend, commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions._

  val international = "int"

  //TODO: is this being used anywhere?
//  val catalog = tpBackend.catalogService.catalog.map(_.valueOr(e => throw new IllegalStateException(s"$e while getting catalog")))

  def index(country: Option[Country]) = NoCacheAction { implicit request =>
    val maybeCountry = country orElse request.getFastlyCountry
    Redirect(routes.WeeklyLandingPage.withCountry(maybeCountry.map(_.alpha2).getOrElse(international)).url, request.queryString, TEMPORARY_REDIRECT)
  }

  val description = landing_description()

  def withCountry(country: String) = NoCacheAction.async { implicit request =>
    val parsedCountry = if (country == international) Some(Country.UK) else CountryGroup.countryByCode(country)
    parsedCountry.fold {
      Future.successful(Redirect(routes.WeeklyLandingPage.withCountry(international).url, request.queryString, PERMANENT_REDIRECT))
    } { country =>
      Future.successful(Redirect(routes.PromoLandingPage.render("10ANNUAL", Some(country)).url, request.queryString, TEMPORARY_REDIRECT))
    }
  }

}
