package views.support

import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PercentDiscount._
import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.memsub.{Price, BillingPeriod => Period}
import com.gu.subscriptions.{DigipackCatalog, DigipackPlan}

import scalaz.NonEmptyList
import scalaz.std.option._
import scalaz.syntax.applicative._
import scalaz.syntax.std.option._

object Catalog {
  import DigipackCatalog.Val

  case class Diagnostic(comparisonTable: Option[ComparisonTable], errorTables: Seq[ErrorTable])
  object Diagnostic {
    def fromCatalogs(testCat: Val[DigipackCatalog], normalCat: Val[DigipackCatalog]): Diagnostic = {
      val test = "test"
      val normal = "normal"

      val comparisonTable =
        (ComparisonTable.fromCatalog(testCat, test) |@| ComparisonTable.fromCatalog(normalCat, normal))(_ interleave _)

      val errorsTables =
        Seq(ErrorTable.fromCatalog(testCat, test), ErrorTable.fromCatalog(normalCat, normal)).flatten

      Diagnostic(comparisonTable, errorsTables)
    }
  }

  case class PlanDescription(name: String, env: String, productRatePlanId: ProductRatePlanId, prices: Seq[Price])

  object PlanDescription {
    def fromPlanDetails(env: String)(productRatePlanId: ProductRatePlanId, plan: DigipackPlan[Period]): PlanDescription = {
      val prices = plan.pricing.underlying.values.toSeq
      PlanDescription(plan.name, env, productRatePlanId, prices)
    }
  }

  case class PlanError(name: String, errorMsg: String)

  case class ComparisonTable(rows: Seq[PlanDescription]) {
    def interleave(other: ComparisonTable) =
      ComparisonTable(
        rows.zip(other.rows)
          .flatMap { case p => Seq(p._1, p._2) }
          .sortBy { r => r.name + r.env }
      )
  }

  object ComparisonTable {
    def fromCatalog(catalog: Val[DigipackCatalog], env: String): Option[ComparisonTable] =
      catalog.fold(_ => None, c =>
        ComparisonTable(c.planMap.toSeq.map { case (prpId, plan) =>
          PlanDescription.fromPlanDetails(env)(prpId, plan)
        }).some
      )
  }

  case class ErrorTable(env: String, rows: NonEmptyList[PlanError])

  object ErrorTable {
    def fromCatalog(catalog: Val[DigipackCatalog], env: String): Option[ErrorTable] =
      catalog.fold(errs =>
        ErrorTable(
          env, errs.map { case (bp, msg) => PlanError(bp.adverb, msg)}
        ).some, _ => None)
  }

  def formatPrice(catalog: DigipackCatalog, promotion: AnyPromotion): String = {
    import catalog.digipackMonthly._
    promotion.asDiscount.fold(priceGBP.pretty)(_.promotionType.applyDiscount(priceGBP, billingPeriod).pretty)
  }

  def formatPrice(catalog: DigipackCatalog): String = {
    import catalog.digipackMonthly._
    priceGBP.pretty
  }
}
