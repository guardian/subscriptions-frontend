package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def index(edition: DigitalEdition) = Action {
    Ok(views.html.index_intl(edition))
  }

  def indexAu() = index(DigitalEdition.AU)

  def indexUs() = index(DigitalEdition.US)
}