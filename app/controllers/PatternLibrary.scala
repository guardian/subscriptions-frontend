package controllers

import actions.CommonActions
import play.api.mvc.Controller

class PatternLibrary(commonActions: CommonActions) extends Controller {

  import commonActions.NoCacheAction

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns())
  }

}
