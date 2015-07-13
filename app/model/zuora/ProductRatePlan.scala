package model.zuora


case class SubscriptionProduct(
  product: ProductPlan,
  frequency: BillingFrequency,
  ratePlanId: String,
  price: Float)

sealed abstract class ProductPlan
object ProductPlan {
  case object Digital extends ProductPlan
}

sealed abstract class BillingFrequency {
  val lowercase = toString.toLowerCase
}

object BillingFrequency {
  case object Month extends BillingFrequency
  case object Quarter extends BillingFrequency
  case object Annual extends BillingFrequency

  val all = Seq[BillingFrequency](Month, Quarter, Annual)
}
