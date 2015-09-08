package acceptance

import acceptance.features.{PrintSubscriptionsSpec, CheckoutSpec}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, Suites}

object Main {
  val suites = Seq(
    new CheckoutSpec,
    new PrintSubscriptionsSpec
  )
}

import acceptance.Main._
class Main extends Suites(suites: _*) with BeforeAndAfterAll with WebBrowser {
  implicit lazy val driver: WebDriver = Config.driver

  override def afterAll(): Unit = {
    quit()
  }
}
