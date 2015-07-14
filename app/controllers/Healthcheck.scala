package controllers

import play.api.mvc._
import services.TouchpointBackend

object Healthcheck extends Controller {

  def index = Action {
    Cached(1) {
      if(TouchpointBackend.Normal.zuoraService.products.nonEmpty) Ok("OK")
      else ServiceUnavailable("Service Unavailable")
    }
  }

}
