package controllers

import actions.CommonActions
import play.api.mvc.Controller

class PatternLibrary extends Controller with CommonActions{

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns())
  }

}
