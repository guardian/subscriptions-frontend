package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.QueryParameter
import play.api.libs.json._

trait QueryParameterInstances {
  // Ignore IntelliJ - these imports are used!
  import cats.syntax.either._

  implicit val queryParameterDecoder: Decoder[QueryParameter] = decodeThriftStruct[QueryParameter]

  implicit val queryParameterEncoder: Encoder[QueryParameter] = encodeThriftStruct[QueryParameter]

  implicit val queryParameterReads: Reads[QueryParameter] = {
    import play.api.libs.functional.syntax._
    ((__ \ "name").read[String] and (__ \ "value").read[String]) { (name, value) =>
      QueryParameter(name, value)
    }
  }

  implicit val queryParameterWrites: Writes[QueryParameter] = Writes { queryParameter =>
    Json.obj("name" -> queryParameter.name, "value" -> queryParameter.value)
  }
}
