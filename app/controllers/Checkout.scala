package controllers

import actions.CommonActions.GoogleAuthenticatedStaffAction
import play.api.mvc._

object Checkout extends Controller {

  def renderCheckout = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.payment()))

  def handleCheckout = GoogleAuthenticatedStaffAction(Redirect(routes.Checkout.thankyou()))

  def thankyou = GoogleAuthenticatedStaffAction(Ok(views.html.checkout.thankyou()))
}