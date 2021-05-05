package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.PaymentProvider

trait PaymentProviderInstances {

  implicit val paymentProviderDecoder: Decoder[PaymentProvider] = decodeThriftEnum[PaymentProvider]

  implicit val paymentProviderEncoder: Encoder[PaymentProvider] = encodeThriftEnum[PaymentProvider]
}
