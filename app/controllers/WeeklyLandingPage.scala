package controllers

import actions.CommonActions._
import model.WeeklyRegion
import play.api.mvc._
import services.TouchpointBackend

object WeeklyLandingPage extends Controller {

  val tpBackend = TouchpointBackend.Normal
  val catalog = tpBackend.catalogService.unsafeCatalog

  def index = CachedAction { implicit request =>
    Ok(views.html.weekly.weekly_landing(WeeklyRegion.all(catalog)))
}}
