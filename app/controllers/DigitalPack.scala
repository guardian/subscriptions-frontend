package controllers

import actions.CommonActions.CachedAction
import model.DigitalEdition
import model.DigitalEdition.{INT, UK, AU, US}
import play.api.mvc._
import services.TouchpointBackend
import views.support.Pricing._

object DigitalPack extends Controller {
  def uk = landingPage(UK)
  def us = landingPage(US)
  def au = landingPage(AU)
  def int = landingPage(INT)

  def selectUk = country(UK)
  def selectUs = country(US)
  def selectAu = country(AU)
  def selectInt = country(INT)

  def landingPage(digitalEdition: DigitalEdition) = CachedAction {
    val plan = TouchpointBackend.Normal.catalogService.digipackCatalog.digipackMonthly
    val price = plan.pricing.getPrice(digitalEdition.countryGroup.currency).getOrElse(plan.gbpPrice)
    Ok(views.html.digitalpack.info(digitalEdition, price))
  }

  def country(digitalEdition: DigitalEdition) = CachedAction {
    Ok(views.html.digitalpack.country(digitalEdition))
  }
}
