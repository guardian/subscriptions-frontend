package controllers

import actions.OAuthActions
import play.api.mvc._

object Checkout extends Controller with OAuthActions {

  def renderCheckout = googleAuthenticatedStaffAction(Ok(views.html.checkout.payment()))

  def handleCheckout = googleAuthenticatedStaffAction(Redirect(routes.Checkout.thankyou()))

  def thankyou = googleAuthenticatedStaffAction(Ok(views.html.checkout.thankyou()))
}