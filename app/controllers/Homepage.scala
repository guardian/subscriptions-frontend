package controllers

import actions.CommonActions.CachedAction
import model.DigitalEdition
import play.api.mvc._

object Homepage extends Controller {

  def index = CachedAction(Ok(views.html.index(DigitalEdition.getMembershipLandingPage(DigitalEdition.UK))))

  def index(edition: DigitalEdition) = CachedAction(Ok(views.html.index_intl(edition, DigitalEdition.getMembershipLandingPage(edition))))

  def indexAu() = index(DigitalEdition.AU)

  def indexUs() = index(DigitalEdition.US)
}
