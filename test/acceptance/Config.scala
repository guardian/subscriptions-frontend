package acceptance

import java.net.URL

import com.typesafe.config.ConfigFactory
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.{Platform, WebDriver}
import org.slf4j.LoggerFactory

import scala.util.Try

object Config {
  def logger = LoggerFactory.getLogger(this.getClass)

  private val conf = ConfigFactory.load()
  val baseUrl = conf.getString("subscriptions.url")
  val profileUrl = conf.getString("identity.webapp.url")
  val testUsersSecret = conf.getString("identity.test.users.secret")

  val driver: WebDriver = {
    Try { new URL(conf.getString("webDriverRemoteUrl")) }.toOption.map { url =>
      val capabilities = DesiredCapabilities.chrome()
      capabilities.setCapability("platform", Platform.WIN8)
      capabilities.setCapability("name", "subscription-frontend: https://github.com/guardian/subscriptions-frontend")
      new RemoteWebDriver(url, capabilities)
    }.getOrElse {
      new ChromeDriver()
    }
  }

  def webDriverSessionId(): String = {
    Config.driver match {
      case remoteDriver: RemoteWebDriver => remoteDriver.getSessionId.toString
      case _ => throw new ClassCastException
    }
  }

  def stage(): String = {
    conf.getString("stage")
  }

  def debug() = {
    conf.root().render()
  }

  def printSummary(): Unit = {
    logger.info("Acceptance Test Configuration")
    logger.info("=============================")
    logger.info(s"Stage: ${stage}")
    logger.info(s"Subscription Frontend: ${Config.baseUrl}")
    logger.info(s"Identity Frontend: ${conf.getString("identity.webapp.url")}")
//    logger.info(s"Identity API: ${conf.getString("identity.baseUri")}")
    logger.info(s"WebDriver Session ID = ${Config.webDriverSessionId}")
  }
}
