package acceptance.util

import java.net.URL
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.{Cookie, WebDriver}
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import scala.collection.JavaConverters._
import io.github.bonigarcia.wdm.ChromeDriverManager

object Driver {
  def apply() = driver
  val sessionId = driver.asInstanceOf[RemoteWebDriver].getSessionId.toString
  def manage() = driver.manage()
  def get(s: String) = driver.get(s)
  def quit() = driver.quit()

  def reset() = {
    Driver.manage().deleteAllCookies()
    Driver.get(s"${Config.baseUrl}/healthcheck")
  }

  def cookiesSet: Set[Cookie] = manage().getCookies.asScala.toSet

  def addCookie(name: String, value: String) = manage().addCookie(new Cookie(name, value))

  private lazy val driver: WebDriver =
    if (Config.webDriverRemoteUrl.isEmpty)
      instantiateLocalBrowser()
    else
      instantiateRemoteBrowser()

  private def instantiateLocalBrowser(): WebDriver = {
    ChromeDriverManager.getInstance().setup()
    new ChromeDriver()
  }

  private def instantiateRemoteBrowser(): WebDriver = {
    val caps = DesiredCapabilities.chrome()
    caps.setCapability("platform", "WINDOWS")
    caps.setCapability("name", "subscription-frontend")
    new RemoteWebDriver(new URL(Config.webDriverRemoteUrl), caps)
  }
}
