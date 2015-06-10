package testUtils

import play.api.libs.json.{JsValue, JsNull}
import play.api.libs.ws.{WSResponse, WSCookie}

import scala.xml.Elem

case class FakeWSResponse(allHeaders: Map[String, Seq[String]] = Map(),
                          status: Int = 200,
                          statusText: String = "200",
                          cookies: Seq[WSCookie] = Seq(),
                          body: String = "",
                          xml: Elem = <xml></xml>,
                          json: JsValue = JsNull,
                          header: Option[String] = None,
                          cookie: Option[WSCookie] = None) extends WSResponse {
  override def underlying[T]: T = ???

  override def header(key: String): Option[String] = header

  override def cookie(name: String): Option[WSCookie] = cookie

  override def bodyAsBytes: Array[Byte] = body.getBytes
}
