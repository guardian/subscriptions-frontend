package controllers

import actions.CommonActions
import com.gu.i18n.CountryGroup.UK
import com.typesafe.scalalogging.StrictLogging
import model.DigitalEdition
import model.DigitalEdition._
import play.api.mvc._
import services.{TouchpointBackend, TouchpointBackends}
import utils.RequestCountry._

import scala.concurrent.ExecutionContext.Implicits.global

class DigitalPack(touchpointBackend: TouchpointBackend, commonActions: CommonActions) extends Controller with StrictLogging {

  import commonActions.NoCacheAction
  private val queryParamHint = "edition"

  private def redirectResult(digitalEdition: DigitalEdition)(implicit request: Request[AnyContent]) = {
    val queryString = request.queryString - queryParamHint
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

  def landingPage(code: String) = NoCacheAction.async { _ =>
    val digitalEdition = getById(code).getOrElse(INT)
    touchpointBackend.catalogService.catalog.map(_.map { catalog =>
      val plan = catalog.digipack.month
      val price = plan.charges.price.getPrice(digitalEdition.countryGroup.currency).getOrElse(plan.charges.gbpPrice)
      Ok(views.html.digitalpack.info(digitalEdition, price))
    }.valueOr { err =>
      logger.error(s"Failed to load the Digital Pack landing page: ${err.list.toList.mkString(", ")}")
      InternalServerError("failed to read catalog, see the logs")
    })
  }

}
