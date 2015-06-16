package utils

import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import scala.io.Source

object Asset {
  private val logger = Logger(this.getClass)

  val map: Map[String, String] = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse)
    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse {
      logger.warn("Could not find a config/assets.map file. You can generate it by running 'grunt compile'")
      Map.empty
    }
  }

  def at(path: String) = "/assets/" + map.getOrElse(path, path)
}
