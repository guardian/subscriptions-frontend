package controllers

import java.util.Date

import app.BuildInfo
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import services.TouchpointBackend

object Management extends Controller with LazyLogging {

  def healthcheck = Action.async {
      (for {
        products <- TouchpointBackend.Normal.catalogService.products if products.nonEmpty
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
      "Products" -> TouchpointBackend.Normal.catalogService.products.value
    )

    Cached(1)(Ok(data map { case (k, v) => s"$k: $v"} mkString "\n"))
  }

}
