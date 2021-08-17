package utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.{WSCookie, WSResponse}

import java.net.URI
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

  override def uri: URI = ???

  def xml: Elem = XML.loadString(body)

  override def header(key: String): Option[String] =
    allHeaders.get(key).flatMap(_.headOption)

  def cookie(name: String): Option[WSCookie] = None

  def bodyAsBytes: ByteString = ByteString(body.getBytes)

  override def headers: Map[String, Seq[String]] = allHeaders

  override def bodyAsSource: Source[ByteString, _] = Source.single(bodyAsBytes)

}
