package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.AcquisitionSource
import play.api.libs.json.{Reads, Writes}

trait AcquisitionSourceInstances extends ThriftEnumFormat {

  implicit val acquisitionSourceDecoder: Decoder[AcquisitionSource] = decodeThriftEnum[AcquisitionSource]

  implicit val acquisitionSourceEncoder: Encoder[AcquisitionSource] = encodeThriftEnum[AcquisitionSource]

  implicit val acquisitionSourceReads: Reads[AcquisitionSource] = thriftEnumReads(AcquisitionSource.valueOf)

  implicit val acquisitionSourceWrites: Writes[AcquisitionSource] = thriftEnumWrites[AcquisitionSource]
}
