package views.support

import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PercentDiscount._
import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.memsub.{Price, BillingPeriod => Period}
import com.gu.memsub.subsv2.{Catalog => Cat, CatalogPlan}
import com.gu.memsub.Month
import scalaz.{NonEmptyList, \/}
import scalaz.std.option._
import scalaz.syntax.applicative._
import scalaz.syntax.std.option._

object Catalog {
  type Val[A] = NonEmptyList[String] \/ A
  case class Diagnostic(comparisonTable: Option[ComparisonTable], errorTables: Seq[ErrorTable])
  object Diagnostic {
    def fromCatalogs(testCat: Val[Cat], normalCat: Val[Cat]): Diagnostic = {
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
    def fromPlanDetails(env: String)(plan: CatalogPlan.Paid): PlanDescription = {
      val prices = plan.charges.price.underlying.values.toSeq
      PlanDescription(plan.name, env, plan.id, prices)
    }
  }

  case class PlanError(name: String, errorMsg: String)

  case class ComparisonTable(rows: Seq[PlanDescription]) {
    def interleave(other: ComparisonTable) =
      ComparisonTable(
        rows.zip(other.rows)
          .flatMap(p => Seq(p._1, p._2))
          .sortBy { r => r.name + r.env }
      )
  }

  object ComparisonTable {
    def fromCatalog(catalog: Val[Cat], env: String): Option[ComparisonTable] =
      catalog.fold(_ => None, c =>
        ComparisonTable(c.allSubs.flatten.map { case (plan) =>
          PlanDescription.fromPlanDetails(env)(plan)
        }).some
      )
  }

  case class ErrorTable(env: String, rows: NonEmptyList[PlanError])

  object ErrorTable {
    def fromCatalog(catalog: Val[Cat], env: String): Option[ErrorTable] =
      catalog.fold(errs =>
        ErrorTable(
          env, errs.map { case (msg) => PlanError("error", msg)}
        ).some, _ => None)
  }

  def formatPrice(plan: CatalogPlan.Digipack[Month], promotion: AnyPromotion): String = {
    promotion.asDiscount.fold(plan.charges.gbpPrice.pretty)(_.promotionType.applyDiscount(plan.charges.gbpPrice, plan.charges.billingPeriod).pretty)
  }

  def formatPrice(plan: CatalogPlan.Digipack[Month]): String = {
    plan.charges.gbpPrice.pretty
  }
}
