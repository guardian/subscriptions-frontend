package controllers
import java.util.Date
import javax.inject.Inject

import app.BuildInfo

import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.TouchpointBackends
import actions.OAuthActions
import akka.actor.ActorSystem
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger._
import views.support.Catalog._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import com.github.nscala_time.time.Imports._
import play.api.libs.ws.WSClient

import scalaz.{Semigroup, Validation, ValidationNel}
import scalaz.syntax.std.option._

class Management(override val wsClient: WSClient, actorSystem: ActorSystem, fBackendFactory: TouchpointBackends)  extends Controller with LazyLogging with OAuthActions {

  implicit val as: ActorSystem = actorSystem
  implicit val unitSemigroup = Semigroup.firstSemigroup[Unit]
  import fBackendFactory.Normal._

  def catalog = GoogleAuthenticatedStaffAction.async { implicit request =>
    val Seq(testCat, normalCat) = Seq(fBackendFactory.Test, fBackendFactory.Normal).map { be =>
      be.catalogService.catalog
    }
    testCat.zip(normalCat).map { case (test, normal) =>
      Ok(views.html.staff.catalog(Diagnostic.fromCatalogs(test, normal)))
    }
  }
  private def salesforce = {
    fBackendFactory.Normal.salesforceService.isAuthenticated
    //The authagent holds an option type, so if it's not been initialised then it will hold None
  }
  private def catalogWorkedOkay: ValidationNel[String, Unit] = (for {
    catalogTry <- catalogService.catalog.value \/> "Catalog not parsed from Zuora yet".wrapNel
    catalogDisjunction <- catalogTry.toOption \/> catalogTry.failed.map(_.getMessage).getOrElse("").wrapNel
    catalog <- catalogDisjunction
  } yield ()).validation

  def boolTest(result: Boolean, name: String): ValidationNel[String, Unit] =
    result.fold(Validation.success(()), Validation.failureNel(s"Test $name failed. health check will fail"))

  private def tests =
    boolTest(salesforce,"Salesforce") +++
    boolTest(CloudWatchHealth.hasPushedMetricSuccessfully, "CloudWatch") +++
    boolTest(zuoraService.lastPingTimeWithin(2.minutes), "ZuoraPing") +++
    boolTest(promoCollection.all.nonEmpty, "Promotions") +++
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
      "Products" -> fBackendFactory.Normal.catalogService
    )

    Cached(1)(Ok(data map { case (k, v) => s"$k: $v" } mkString "\n"))
  }

}
