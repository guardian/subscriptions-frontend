package controllers

import actions.CommonActions.CachedAction
import play.api.mvc._

object Homepage extends Controller {

  def index = CachedAction(Ok(views.html.index()))

  def index(edition: DigitalEdition) = CachedAction(Ok(views.html.index_intl(edition)))

  def indexAu() = index(DigitalEdition.AU)

  def indexUs() = index(DigitalEdition.US)
}