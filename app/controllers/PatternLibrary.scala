package controllers

import play.api.mvc.Controller
import actions.CommonActions.NoCacheAction

object PatternLibrary extends Controller {

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns())
  }

}
