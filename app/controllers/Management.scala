package controllers

import java.util.Date

import app.BuildInfo
import com.gu.memsub.services.CatalogService
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.TouchpointBackend
import actions.CommonActions._
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger._
import views.support.Catalog._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import com.github.nscala_time.time.Imports._
import com.gu.memsub.subsv2.Catalog
import scalaz.syntax.std.option._
import scalaz.\/

object Management extends Controller with LazyLogging {
  implicit val as = Akka.system

  def catalog = GoogleAuthenticatedStaffAction.async { implicit request =>
    val Seq(testCat, normalCat) = Seq(TouchpointBackend.Test, TouchpointBackend.Normal).map { be =>
      be.catalogService.catalog
    }
    testCat.zip(normalCat).map { case (test, normal) =>
      Ok(views.html.staff.catalog(Diagnostic.fromCatalogs(test, normal)))
    }
  }

  import TouchpointBackend.Normal._

  trait Test {
    def ok: Boolean
    def messages: Seq[String] = Nil
  }

  class BoolTest(name: String, exec: () => Boolean) extends Test {
    override def messages = List(s"Test $name failed, health check will fail")
    override def ok = exec()
  }

  class EitherTest[A](name: String, exec: () => List[String] \/ A) extends Test {
    override def messages = exec().swap.toOption.toList.flatten
    override def ok: Boolean = exec().isRight
  }

  private def catalogTest: List[String] \/ Catalog = for {
    catalogTry <- catalogService.catalog.value \/> List("Future not completed yet")
    disjunction <- catalogTry.toOption \/> List(catalogTry.failed.map(_.getMessage).getOrElse(""))
    catalog <- disjunction.leftMap(_.list)
  } yield catalog

  private def tests = Seq(
    new BoolTest("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    new BoolTest("ZuoraPing", () => zuoraService.lastPingTimeWithin(2.minutes)),
    new BoolTest("Promotions", () => promoCollection.all.nonEmpty),
    new EitherTest("CatalogSuccess", catalogTest _)
  )

  def healthcheck() = Action {
    Cached(1) {
      val failures = tests.filterNot(_.ok)
      if (failures.isEmpty) {
        Ok("OK")
      } else {
        failures.flatMap(_.messages).foreach(msg => warn(msg))
        ServiceUnavailable("Service Unavailable")
      }
    }
  }

  def manifest() = Action {
    val data = Map(
      "Build" -> BuildInfo.buildNumber,
      "Date" -> new Date(BuildInfo.buildTime).toString,
      "Commit" -> BuildInfo.gitCommitId,
      "Products" -> TouchpointBackend.Normal.catalogService
    )

    Cached(1)(Ok(data map { case (k, v) => s"$k: $v"} mkString "\n"))
  }

}
