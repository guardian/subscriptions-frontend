package utils

import akka.util.ByteString
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.{WSCookie, WSResponse}

import scala.xml.{Elem, XML}

case class TestWSResponse (
    allHeaders: Map[String, Seq[String]] = Map.empty,
    statusText: String = "OK",
    body: String = "",
    cookies: Seq[WSCookie] = Seq(),
    json: JsValue = JsNull,
    status: Int = 200
    ) extends WSResponse {

  def underlying[T]: T = ???

  def xml: Elem = XML.loadString(body)

  def header(key: String): Option[String] =
    allHeaders.get(key).flatMap(_.headOption)

  def cookie(name: String): Option[WSCookie] = None

  def bodyAsBytes: ByteString = ByteString(body.getBytes)
}
