package controllers

import actions.CommonActions
import play.api.mvc.{BaseController, ControllerComponents}

class PatternLibrary(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.NoCacheAction

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns())
  }

}
