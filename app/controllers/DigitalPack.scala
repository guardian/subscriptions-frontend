package controllers

import actions.CommonActions.CachedAction
import model.DigitalEdition
import model.DigitalEdition.{UK, AU, US}
import play.api.mvc._


object DigitalPack extends Controller {


  def uk = landingPage(UK)
  def us = landingPage(US)
  def au = landingPage(AU)

  def selectUk = country(UK)
  def selectUs = country(US)
  def selectAu = country(AU)

  def landingPage(digitalEdition: DigitalEdition) = CachedAction {
    Ok(views.html.digitalpack.info(digitalEdition, DigitalEdition.getRedirect(digitalEdition)))
  }

  def country(digitalEdition: DigitalEdition) = CachedAction {
    Ok(views.html.digitalpack.country(digitalEdition))
  }

}
