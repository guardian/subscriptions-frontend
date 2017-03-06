package model

import com.gu.memsub.Benefit.{Digipack, Weekly}
import com.gu.memsub.subsv2.SubscriptionPlan

import scala.reflect.internal.util.StringOps

object SubPlanOps {

  implicit class EnrichedSubscriptionPlan[+P <: SubscriptionPlan.AnyPlan](in: P) {
    def packageName: String = in.charges.benefits.list match {
      case Digipack :: Nil => "Guardian Digital Pack"
      case Weekly :: Nil => "The Guardian Weekly"
      case _ => s"${in.name} package"
    }

    def subtitle: Option[String] = in.charges.benefits.list match {
      case Digipack :: Nil => Some("Daily Edition + Guardian App Premium Tier")
      case _ => StringOps.oempty(in.description).headOption
    }
  }

}
