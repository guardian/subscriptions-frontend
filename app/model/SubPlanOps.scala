package model

import com.gu.memsub.Benefit.{Digipack, Weekly}
import com.gu.memsub.subsv2.{CatalogPlan, SubscriptionPlan}

import scala.reflect.internal.util.StringOps

object SubPlanOps {

  implicit class EnrichedSubscriptionPlan[+P <: SubscriptionPlan.AnyPlan](in: P) {
    def asCatalogPlan: Option[CatalogPlan.Paid] = {

    }
  }

}
