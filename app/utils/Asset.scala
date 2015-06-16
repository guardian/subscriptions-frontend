package utils

import play.api.libs.json.{JsObject, Json}

import scala.io.Source

object Asset {
  lazy val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse)
    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse(Map.empty)
  }

  def at(path: String) = "/assets/" + map.getOrElse(path, path)
}
