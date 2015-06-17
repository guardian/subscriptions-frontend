package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

object CachedAssets extends Controller {
  private val logger = Logger(getClass)
  private val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt     = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse)

    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse {
      logger.warn("Could not find a config/assets.map file. You can generate it by running 'grunt compile'")
      Map.empty
    }
  }

  def at(path: String, file: String, aggressiveCaching: Boolean = false) = Action.async { request =>
    val hashedFile = map.getOrElse(file, file)

    controllers.Assets.at(path, hashedFile, aggressiveCaching).apply(request).recover {
      case e: RuntimeException => {
        Logger.warn(s"Asset run time exception for path $path $file. Does this file exist?", e)
        Cached(NotFound)
      }
    }.map { result =>
      if (result.header.headers.contains(CACHE_CONTROL)) result else Cached(2)(result)
    }
  }
}
