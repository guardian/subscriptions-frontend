package controllers

import actions.CommonActions
import play.api.mvc.Controller

object PatternLibrary extends Controller with CommonActions{

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns())
  }

}
