package com.gu.acquisition.model

import play.api.libs.json.{Reads, Writes, Json => PlayJson}

/**
  * Ids to be included in requests to Ophan.
  */
case class OphanIds(pageviewId: Option[String], visitId: Option[String], browserId: Option[String])

object OphanIds {
  import io.circe._
  import io.circe.generic.semiauto._

  implicit val decoder: Decoder[OphanIds] = deriveDecoder[OphanIds]

  implicit val encoder: Encoder[OphanIds] = deriveEncoder[OphanIds]

  implicit val reads: Reads[OphanIds] = PlayJson.reads[OphanIds]

  implicit val writes: Writes[OphanIds] = PlayJson.writes[OphanIds]
}
