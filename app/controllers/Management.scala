package controllers

import java.util.Date

import app.BuildInfo
import scalaz.syntax.nel._
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
import scalaz.syntax.applicative._
import scalaz.syntax.std.option._
import scalaz.{NonEmptyList, Validation, ValidationNel, \/}

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
    def run: ValidationNel[String, Unit]
  }

  class BoolTest(name: String, exec: () => Boolean) extends Test {
    override def run = if(exec())
      Validation.success(())
    else
      Validation.failureNel(s"Test $name failed, health check will fail")
  }

  class EitherTest[A](name: String, exec: () => NonEmptyList[String] \/ A) extends Test {
    override def run = exec().validation.map(_ => ())
  }

  private def catalogTest: NonEmptyList[String] \/ Catalog = for {
    catalogTry <- catalogService.catalog.value \/> "Future not completed yet".wrapNel
    catalogDisjunction <- catalogTry.toOption \/> catalogTry.failed.map(_.getMessage).getOrElse("").wrapNel
    catalog <- catalogDisjunction
  } yield catalog

  private def tests = (
    new BoolTest("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully).run |@|
    new BoolTest("ZuoraPing", () => zuoraService.lastPingTimeWithin(2.minutes)).run |@|
    new BoolTest("Promotions", () => promoCollection.all.nonEmpty).run |@|
    new EitherTest("CatalogSuccess", catalogTest _).run
  ).tupled

  def healthcheck() = Action {
    Cached(1) {
      tests.fold(
        errors => {
          errors.list.foreach(warn(_))
          ServiceUnavailable("Service Unavailable")
        },
        _ => Ok("OK")
      )
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
