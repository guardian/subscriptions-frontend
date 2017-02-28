package views.support
import com.gu.memsub._
import com.gu.memsub.subsv2.CatalogPlan
import com.gu.memsub.subsv2.CatalogPlan.{ContentSubscription, RecurringPlan}


case class PlanList[+A](associations: List[(ContentSubscription, ContentSubscription)], default: A, others: A*) {
  def map[B](f: A => B): PlanList[B] = PlanList(associations, f(default), others.map(f):_*)
  def list = Seq(default) ++ others
  def associationFor(p: ContentSubscription) =
    associations.toMap.get(p)
}

case class ProductPopulationData(deliveryAddress: Option[Address], plans: PlanList[CatalogPlan.ContentSubscription])
