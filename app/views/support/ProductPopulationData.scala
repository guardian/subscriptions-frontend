package views.support
import com.gu.memsub._
import com.gu.subscriptions.{ChargeName, DigipackPlan, PaperPlan}

case class PlanList[+A](default: A, others: A*) {
  def map[B](f: A => B): PlanList[B] = PlanList(f(default), others.map(f):_*)
  def list = Seq(default) ++ others
}

sealed trait ProductPopulationData {
  def plans: PlanList[PaidPlan[Current, BillingPeriod]]
  def family: ProductFamily
}

case class DigipackProductPopulationData(
  plans: PlanList[DigipackPlan[BillingPeriod]]
) extends ProductPopulationData {
  val family = Digipack
}

case class PaperProductPopulationData(
  deliveryAddress: Option[Address],
  plans: PlanList[PaperPlan[Current, ChargeName]]
) extends ProductPopulationData {
  val family = Paper
}

object ProductPopulationData {
  implicit class EitherySubscriptionsForm(in: ProductPopulationData) {
    def toPlanEither: PlanList[Either[DigipackPlan[BillingPeriod], PaperPlan[Current, ChargeName]]] = in match {
      case DigipackProductPopulationData(plans) => plans.map(Left(_))
      case PaperProductPopulationData(_, plans) => plans.map(Right(_))
    }
    def paper: Option[PaperProductPopulationData] = Some(in).collect { case e: PaperProductPopulationData => e }
    def digipack: Option[DigipackProductPopulationData] = Some(in).collect { case e: DigipackProductPopulationData => e }
    def fold[A](f: PaperProductPopulationData => A)(g: DigipackProductPopulationData => A) = {
      in match {
        case a: DigipackProductPopulationData => g(a)
        case b: PaperProductPopulationData => f(b)
      }
    }
  }
}
