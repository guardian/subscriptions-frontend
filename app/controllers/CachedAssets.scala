package controllers

import com.typesafe.scalalogging.LazyLogging
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
object CachedAssets extends LazyLogging  {
  val hashedPaths: Map[String, String] =
    (for {
      resourceIs <- Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    } yield Json.parse(resourceIs).as[Map[String, String]]).getOrElse {

      logger.warn("Could not find a config/assets.map file. You can generate it by running 'grunt compile'")
      Map.empty
    }

  /**
    * Returns a hashed asset *path* suitable for rending out to the browser - forcing them to fetch new
    * assets when the hash changes.
    */
  def hashedPathFor(path: String): String = "/assets/" + hashedPaths.getOrElse(path, path)
}
class CachedAssets extends Controller with LazyLogging {
  /**
   * Serves an asset, wrapping it with cache headers if appropriate
   */
  def at(path: String, file: String, aggressiveCaching: Boolean = false) = Action.async { request =>
    controllers.Assets.at(path, file, aggressiveCaching).apply(request).recover {
      case e: RuntimeException => {
        Logger.warn(s"Asset run time exception for path $path $file. Does this file exist?", e)
        Cached(NotFound)
      }
    }.map { result =>
      if (result.header.headers.contains(CACHE_CONTROL)) result else Cached(2)(result)
    }
  }
  def busted(path: String) = at("/public", CachedAssets.hashedPaths(path), aggressiveCaching = true)
}
