package acceptance.pages

import acceptance.util.Config.baseUrl
import acceptance.util.{Browser, Config, TestUser}
import org.openqa.selenium.{By, WebElement}
import org.scalatest.selenium.Page

case class WeeklyPromo(endpoint: String = "/p/10ANNUAL", country: String = "GB", params: Option[String] = None) extends Page with Browser {

  val url = s"$baseUrl/$endpoint?country=$country${params.map{p => s"&$p"}.getOrElse("")}"

  private val userDisplayName = cssSelector(".js-user-displayname")
  private val submitPaymentButton = cssSelector(".js-checkout-submit")
  private val localDelivery = cssSelector(".section-slice--bleed.section-right > div :nth-child(1)")

  def pageHasLoaded(): Boolean = pageHasElement(cssSelector(".weekly__package.js-dropdown"))

  object DestinationList {
    private val destinationListSelector = ".section-slice--bleed.section-right > div"
    private val destinationListElement = cssSelector(destinationListSelector).findElement

    private def nthDestinationElementSelector(pos: Int) =
      s"$destinationListSelector :nth-child(${pos.toString})"

    private def nthDestinationElement(pos: Int) = {
      cssSelector(nthDestinationElementSelector(pos)).findElement
    }

    def firstElementAttribute(name: String): String = nthDestinationElement(1).flatMap{ el =>
      println(s"### ${el.underlying.getTagName}")
      el.attribute(name)
    }.getOrElse("")

    def chooseLocalDelivery(): Unit = {
      clickOn(cssSelector(nthDestinationElementSelector(1)))
    }

    def menuIsOpened(name: String): Boolean = {
      destinationListElement.map{ el =>
        val dropDownSelector = s"js-dropdown-$name"
        el.underlying.findElements(By.cssSelector(dropDownSelector))
      }.exists { listItems =>
        if (listItems.isEmpty) {
          false
        } else {
          !listItems.get(0).getCssValue("class").contains("animate-hide-hidden")
        }
      }
    }
  }

}
