package acceptance.util

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scala.util.{Try, Failure, Success}

object Config {
  def logger = LoggerFactory.getLogger(this.getClass)

  private val conf = ConfigFactory.load()

  val baseUrl = conf.getString("subscriptions.url")
  val identityFrontendUrl = conf.getString("identity.webapp.url")
  val testUsersSecret = conf.getString("identity.test.users.secret")
  val webDriverRemoteUrl = Try(conf.getString("webDriverRemoteUrl")) match {
    case Success(url) => url
    case Failure(e) => ""
  }

  def debug() = conf.root().render()

  def printSummary(): Unit = {
    logger.info("Acceptance Test Configuration")
    logger.info("=============================")
    logger.info(s"Stage: ${conf.getString("stage")}")
    logger.info(s"Subscriptions Frontend: ${baseUrl}")
    logger.info(s"Identity Frontend: ${identityFrontendUrl}")
  }
}

