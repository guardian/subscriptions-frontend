package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.AbTestInfo

trait AbTestInfoInstances {
  // Ignore IntelliJ - these imports are used!
  import cats.syntax.either._

  implicit val abTestInfoDecoder: Decoder[AbTestInfo] = decodeThriftStruct[AbTestInfo]

  implicit val abTestInfoEncoder: Encoder[AbTestInfo] = encodeThriftStruct[AbTestInfo]
}
