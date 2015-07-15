package model.zuora


case class SubscriptionProduct(
  product: DigitalProductPlan,
  frequency: BillingFrequency,
  ratePlanId: String,
  price: Float)


case class DigitalProductPlan(id: String)

sealed abstract class BillingFrequency {
  val lowercase = toString.toLowerCase
}

object BillingFrequency {
  case object Month extends BillingFrequency
  case object Quarter extends BillingFrequency
  case object Annual extends BillingFrequency

  val all = Seq[BillingFrequency](Month, Quarter, Annual)
}
