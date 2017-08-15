package model

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

case class FulfilmentLookup(subscriptionInFile: Boolean, addressDetails: Option[String])

object FulfilmentLookup {

  implicit val fulfilmentLookupReads: Reads[FulfilmentLookup] = (
    (JsPath \ "subscriptionInFile").read[Boolean] and
    (JsPath \ "addressDetails").readNullable[String]
  )(FulfilmentLookup.apply _)

}
