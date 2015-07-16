package controllers

import actions.CommonActions.NoCacheAction
import play.api.mvc._

object Healthcheck extends Controller {

  def index = NoCacheAction {
    Ok("""{"status":"OK"}""").withHeaders(CONTENT_TYPE -> "application/json")
  }

}