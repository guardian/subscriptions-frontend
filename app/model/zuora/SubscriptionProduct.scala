package model.zuora


case class SubscriptionProduct(product: DigitalProductPlan,
                               frequency: BillingFrequency,
                               ratePlanId: String,
                               price: Float)


case class DigitalProductPlan(id: String)

sealed abstract class BillingFrequency(val numberOfMonths:Int) {
  val lowercase = toString.toLowerCase
}

object BillingFrequency {
  case object Month extends BillingFrequency(1)
  case object Quarter extends BillingFrequency(3)
  case object Annual extends BillingFrequency(12)

  val all = Seq[BillingFrequency](Month, Quarter, Annual)
}
