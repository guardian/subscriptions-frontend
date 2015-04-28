package controllers

import play.api._
import play.api.mvc._

object Checkout extends Controller {

  def renderCheckout = Action {
    Ok(views.html.checkout.payment())
  }

  def handleCheckout = Action {
    Redirect("/checkout/thankyou")
  }

  def thankyou = Action {
    Ok(views.html.checkout.thankyou())
  }

}