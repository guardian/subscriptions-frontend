package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.Acquisition

trait AcquisitionInstances {
  // Ignore IntelliJ - these imports are used!
  import cats.syntax.either._

  implicit val acquisitionDecoder: Decoder[Acquisition] = decodeThriftStruct[Acquisition]

  implicit val acquisitionEncoder: Encoder[Acquisition] = encodeThriftStruct[Acquisition]
}
