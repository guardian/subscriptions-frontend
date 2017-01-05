package controllers
import java.util.Date

import app.BuildInfo

import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
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
import scalaz.{Semigroup, Validation, ValidationNel}
import scalaz.syntax.std.option._

object Management extends Controller with LazyLogging {

  implicit val as = Akka.system
  implicit val unitSemigroup = Semigroup.firstSemigroup[Unit]
  import TouchpointBackend.Normal._

  def catalog = GoogleAuthenticatedStaffAction.async { implicit request =>
    val Seq(testCat, normalCat) = Seq(TouchpointBackend.Test, TouchpointBackend.Normal).map { be =>
      be.catalogService.catalog
    }
    testCat.zip(normalCat).map { case (test, normal) =>
      Ok(views.html.staff.catalog(Diagnostic.fromCatalogs(test, normal)))
    }
  }

  private def catalogWorkedOkay: ValidationNel[String, Unit] = (for {
    catalogTry <- catalogService.catalog.value \/> "Catalog not parsed from Zuora yet".wrapNel
    catalogDisjunction <- catalogTry.toOption \/> catalogTry.failed.map(_.getMessage).getOrElse("").wrapNel
    catalog <- catalogDisjunction
  } yield ()).validation

  def boolTest(result: Boolean, name: String): ValidationNel[String, Unit] =
    result.fold(Validation.success(()), Validation.failureNel(s"Test $name failed. health check will fail"))

  private def tests =
    boolTest(CloudWatchHealth.hasPushedMetricSuccessfully, "CloudWatch") +++
    boolTest(zuoraService.lastPingTimeWithin(2.minutes), "ZuoraPing") +++
    boolTest(promoCollection.futureAll.isCompleted, "Promotions") +++
    catalogWorkedOkay

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

    Cached(1)(Ok(data map { case (k, v) => s"$k: $v" } mkString "\n"))
  }

}
