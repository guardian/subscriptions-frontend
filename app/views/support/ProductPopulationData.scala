package views.support
import com.gu.memsub._
import com.gu.subscriptions.ProductPlan

case class PlanList[+A](default: A, others: A*) {
  def map[B](f: A => B): PlanList[B] = PlanList(f(default), others.map(f):_*)
  def list = Seq(default) ++ others
}


case class ProductPopulationData(deliveryAddress: Option[Address], plans: PlanList[ProductPlan])