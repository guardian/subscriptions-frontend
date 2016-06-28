package views.support
import com.gu.memsub.{Address, BillingPeriod, Current, PaidPlan}
import com.gu.subscriptions.{Day, DigipackPlan, PaperPlan}

case class PlanList[+A](default: A, others: A*) {
  def map[B](f: A => B): PlanList[B] = PlanList(f(default), others.map(f):_*)
  def list = Seq(default) ++ others
}

sealed trait SubscriptionsForm {
  def plans: PlanList[PaidPlan[Current, BillingPeriod]]
}

case class DigipackSubscriptionsForm(
  plans: PlanList[DigipackPlan[BillingPeriod]]
) extends SubscriptionsForm

case class PaperSubscriptionsForm(
  deliveryAddress: Option[Address],
  plans: PlanList[PaperPlan[Current, Day]]
) extends SubscriptionsForm

object SubscriptionsForm {
  implicit class EitherySubscriptionsForm(in: SubscriptionsForm) {
    def toPlanEither: PlanList[Either[DigipackPlan[BillingPeriod], PaperPlan[Current, Day]]] = in match {
      case DigipackSubscriptionsForm(plans) => plans.map(Left(_))
      case PaperSubscriptionsForm(_, plans) => plans.map(Right(_))
    }
    def paper: Option[PaperSubscriptionsForm] = Some(in).collect { case e: PaperSubscriptionsForm => e }
    def digipack: Option[PaperSubscriptionsForm] = Some(in).collect { case e: PaperSubscriptionsForm => e }
  }
}
