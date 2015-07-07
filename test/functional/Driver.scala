package functional

import java.net.URL

import com.typesafe.config.ConfigFactory
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.{Platform, WebDriver}

import scala.util.Try


object Driver {
  def fromConfig: WebDriver = {
    val conf = ConfigFactory.load()
    Try { conf.getString("webDriverRemoteUrl") }.toOption.map { urlS =>
      val url = new URL(urlS)
      new RemoteWebDriver(url, defaultCapabilities)
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
