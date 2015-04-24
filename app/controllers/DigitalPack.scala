package controllers

import play.api._
import play.api.mvc._

object DigitalPack extends Controller {

  def uk = Action {
    Ok(views.html.digitalpack.uk())
  }

}