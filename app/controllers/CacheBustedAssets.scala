package controllers

import play.api.mvc.{Action, AnyContent, Controller}

object CacheBustedAssets extends Controller{
  def at(path: String): Action[AnyContent] =  {
    controllers.Assets.at("/public", CachedAssets.hashedPaths(path), aggressiveCaching = true)
  }
}