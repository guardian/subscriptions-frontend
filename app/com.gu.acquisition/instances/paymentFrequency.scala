package com.gu.acquisition.instances

import com.gu.fezziwig.CirceScroogeMacros._
import io.circe.{Decoder, Encoder}
import ophan.thrift.event.PaymentFrequency

trait PaymentFrequencyInstances {

  implicit val paymentFrequencyDecoder: Decoder[PaymentFrequency] = decodeThriftEnum[PaymentFrequency]

  implicit val paymentFrequencyEncoder: Encoder[PaymentFrequency] = encodeThriftEnum[PaymentFrequency]
}
