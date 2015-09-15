package controllers

import actions.CommonActions.CachedAction
import model.Benefits
import play.api.mvc._

case class DigitalEdition(id: String, name: String, price: String, cmp: String)

object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

}

object DigitalPack extends Controller {

  import DigitalEdition._

  def uk = landingPage(UK)
  def us = landingPage(US)
  def au = landingPage(AU)

  def selectUk = country(UK)
  def selectUs = country(US)
  def selectAu = country(AU)

  def landingPage(digitalEdition: DigitalEdition) = CachedAction {
    Ok(views.html.digitalpack.info(digitalEdition))
  }

  def vue = CachedAction {
    val startUrl = "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=DGA45&CMP=ema-1501"
    Ok(views.html.digitalpack.info(UK, Benefits.digitalPackVue, Some(startUrl)))
  }

  def country(digitalEdition: DigitalEdition) = CachedAction {
    Ok(views.html.digitalpack.country(digitalEdition))
  }

}
