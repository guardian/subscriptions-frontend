package controllers

import actions.CommonActions
import play.api.mvc._

object Homepage extends Controller with CommonActions{

  def index = cachedAction(Ok(views.html.index()))

  def index(edition: DigitalEdition) = cachedAction(Ok(views.html.index_intl(edition)))

  def indexAu() = index(DigitalEdition.AU)

  def indexUs() = index(DigitalEdition.US)
}