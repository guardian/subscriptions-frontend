package controllers

import play.api.mvc._

object Healthcheck extends Controller {

  def index = Action {
    Ok("""{"status":"OK"}""").withHeaders(CONTENT_TYPE -> "application/json")
  }

}