package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.componentEvent.ComponentType
import play.api.libs.json.{Reads, Writes}

trait ComponentTypeInstances extends ThriftEnumFormat {

  implicit val componentTypeDecoder: Decoder[ComponentType] = decodeThriftEnum[ComponentType]

  implicit val componentTypeEncoder: Encoder[ComponentType] = encodeThriftEnum[ComponentType]

  implicit val componentTypeReads: Reads[ComponentType] = thriftEnumReads(ComponentType.valueOf)

  implicit val componentTypeWrites: Writes[ComponentType] = thriftEnumWrites[ComponentType]
}
