package controllers

import actions.CommonActions._
import com.gu.subscriptions.CAS.{CASError, CASSuccess}
import configuration.Config
import forms.CASForm
import play.api.libs.json.Json
import play.api.mvc.Controller
import views.support.CASResultOps._
import play.api.libs.concurrent.Execution.Implicits._

object CAS extends Controller {
  def index = NoCacheAction { implicit request =>
    Ok(views.html.staff.cas())
  }

  def search = NoCacheAction.async(parse.form(CASForm())) { request =>
    val lookup = request.body
    Config.casService.check(lookup.subscriptionNumber, lookup.postcode, lookup.lastName, triggersActivation = false).map {
      case r: CASSuccess => Ok(Json.toJson(r))
      case r: CASError => BadRequest(Json.toJson(r))
    }
  }
}
