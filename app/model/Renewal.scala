package model

import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.CatalogPlan.Paper
import play.api.libs.json._


case class Renewal(email: String, plan: Paper, paymentData: PaymentData)

class RenewalReads(catalog: Catalog) {
  val weeklyPlans = catalog.weeklyZoneA.toList ++ catalog.weeklyZoneB.toList
  implicit val paperReads = new Reads[Paper] {
    override def reads(json: JsValue): JsResult[Paper] = json match {
      case JsString(ratePlanId) => weeklyPlans.find(_.id.get == ratePlanId).map(JsSuccess(_)).getOrElse(JsError("invalid plan"))
      case _ => JsError("invalid plan")
    }
  }
  val directDebitReads = Json.reads[DirectDebitData]
  val creditCardReads = Json.reads[CreditCardData]

  implicit val paymentDataReads = new Reads[PaymentData] {

    def getPaymentType(jsonVal:JsValue) : Option[PaymentType] = jsonVal match {
      case JsObject(paymentData) => paymentData.get("type").flatMap { case JsString(k) => PaymentType.fromKey(k) }
      case _ => None
    }

    override def reads(json: JsValue): JsResult[PaymentData] = getPaymentType(json) match{
        case Some(DirectDebit) => directDebitReads.reads(json)
        case Some(CreditCard) => creditCardReads.reads(json)
        case None => JsError("invalid payment data type")
      }
  }
  implicit val renewalReads = Json.reads[Renewal]
}

