package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.AbTest
import play.api.libs.json._

trait AbTestInstances {
  // Ignore IntelliJ - these imports are used!
  import cats.syntax.either._

  implicit val abTestDecoder: Decoder[AbTest] = decodeThriftStruct[AbTest]

  implicit val abTestEncoder: Encoder[AbTest] = encodeThriftStruct[AbTest]

  implicit val abTestReads: Reads[AbTest] = {
    import play.api.libs.functional.syntax._
    ((__ \ "name").read[String] and (__ \ "variant").read[String]) { (name, variant) =>
      AbTest(name, variant)
    }
  }

  implicit val abTestWrites: Writes[AbTest] = Writes { abTest =>
    Json.obj("name" -> abTest.name, "variant" -> abTest.variant)
  }
}
