package controllers

import actions.CommonActions.{CachedAction, NoCacheAction}
import com.gu.i18n.CountryGroup
import com.netaporter.uri.{PathPart, StringPathPart, Uri}
import com.netaporter.uri.dsl._
import model.DigitalEdition
import model.DigitalEdition._
import play.api.mvc._
import services.TouchpointBackend
import views.support.Pricing._
import utils.RequestCountry._

object DigitalPack extends Controller {
  def uk = landingPage(UK)
  def us = landingPage(US)
  def au = landingPage(AU)
  def int = landingPage(INT)

  def redirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.UK)
    val baseUri = Uri.parse(request.uri)
    val redirectUri = baseUri.copy(pathParts = baseUri.pathParts.+:(StringPathPart(getForCountryGroup(countryGroup).id)))
    Redirect(request.toUriWithCampaignParams(redirectUri), SEE_OTHER)
  }

  def landingPage(digitalEdition: DigitalEdition) = CachedAction {
    val plan = TouchpointBackend.Normal.catalogService.digipackCatalog.digipackMonthly
    val price = plan.pricing.getPrice(digitalEdition.countryGroup.currency).getOrElse(plan.gbpPrice)
    Ok(views.html.digitalpack.info(digitalEdition, price))
  }

}
