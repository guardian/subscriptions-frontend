package functional

import java.net.URL

import com.typesafe.config.ConfigFactory
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.{Platform, WebDriver}

import scala.util.Try


object Config {
  private val conf = ConfigFactory.load()
  val appUrl = conf.getString("subscriptions.url")

  lazy val driver: WebDriver = {
    Try { conf.getString("webDriverRemoteUrl") }.toOption.map { urlS =>
      new RemoteWebDriver(new URL(urlS), defaultCapabilities)
    }.getOrElse {
      new ChromeDriver()
    }
  }

  val defaultCapabilities = {
    val capabilities = DesiredCapabilities.chrome()
    capabilities.setCapability("platform", Platform.WIN8)
    capabilities.setCapability("name", "Subscriptions frontend Acceptance test: https://github.com/guardian/subscriptions-frontend")
    capabilities
  }
}
