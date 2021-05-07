package com.gu.acquisition.model

/**
 * Describes the information that Google Analytics needs to track correctly
 * @param hostname - Used to distinguish the site that this conversion comes from this helps with GA views
 * @param clientId - The GA client id used to identify unique devices, can be retrieved from the _ga cookie
 * @param clientIpAddress - Allows GA to to compute all the geo / network dimensions.
 * @param clientUserAgent - Allows GA to compute the browser, platform, and mobile capabilities dimensions.
 */
case class GAData(
  hostname: String,
  clientId: String,
  clientIpAddress: Option[String],
  clientUserAgent: Option[String]
)

object GAData {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val decoder: Decoder[GAData] = deriveDecoder
  implicit val encoder: Encoder[GAData] = deriveEncoder
}
