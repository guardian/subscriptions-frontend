package controllers

import actions.CommonActions
import play.api.mvc._

object Healthcheck extends Controller with CommonActions {

  def index = noCacheAction {
    Ok("""{"status":"OK"}""").withHeaders(CONTENT_TYPE -> "application/json")
  }

}