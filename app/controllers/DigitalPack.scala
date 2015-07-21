package controllers

import actions.CommonActions.{CachedAction, NoCacheAction}
import play.api.mvc._

case class DigitalEdition(id: String, name: String, price: String, cmp: String)

object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

}

object DigitalPack extends Controller {

  def uk = CachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.UK))
  }

  def us = CachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.US))
  }

  def au = CachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.AU))
  }

  def country = CachedAction {
    Ok(views.html.digitalpack.country())
  }

}
