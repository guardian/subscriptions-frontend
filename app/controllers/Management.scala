package controllers

import java.util.Date

import app.BuildInfo
import com.gu.memsub.services.CatalogService
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.TouchpointBackend
import actions.CommonActions._
import views.support.Catalog._
import play.api.Play.current
import play.api.libs.concurrent.Akka

object Management extends Controller with LazyLogging {
  implicit val as = Akka.system

  def catalog = GoogleAuthenticatedStaffAction.async { implicit request =>
    val Seq(testCat, normalCat) = Seq(TouchpointBackend.Test, TouchpointBackend.Normal).map { be =>
      CatalogService.makeDigipackCatalog(be.zuoraRestClient, be.digipackIds)
    }
    testCat.zip(normalCat).map { case (test, normal) =>
      Ok(views.html.staff.catalog(Diagnostic.fromCatalogs(test, normal)))
    }
  }


  def healthcheck = Action.async {
      (for {
        sfAuth <- TouchpointBackend.Normal.salesforceService.repo.salesforce.getAuthentication
      } yield Ok("OK"))
      .recover { case t: Throwable =>
        logger.warn("Health check failed", t)
        ServiceUnavailable("Service Unavailable")
      }
      .map(Cached(1)(_))
  }

  def manifest() = Action {
    val data = Map(
      "Build" -> BuildInfo.buildNumber,
      "Date" -> new Date(BuildInfo.buildTime).toString,
      "Commit" -> BuildInfo.gitCommitId,
      "Products" -> TouchpointBackend.Normal.catalogService.digipackCatalog
    )

    Cached(1)(Ok(data map { case (k, v) => s"$k: $v"} mkString "\n"))
  }

}
