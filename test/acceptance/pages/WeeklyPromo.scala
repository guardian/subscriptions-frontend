package acceptance.pages

import acceptance.util.Config.baseUrl
import acceptance.util.{Browser, Config, TestUser}
import org.openqa.selenium.{By, WebElement}
import org.scalatest.selenium.Page

case class WeeklyPromo(endpoint: String = "/p/WWM99X", country: String = "GB") extends Page with Browser {

  override val timeOutSec: Int = 5

  val url = s"$baseUrl/$endpoint?country=$country"

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
      el.attribute(name)
    }.getOrElse("")

    def chooseLocalDelivery(): Unit = {
      clickOn(cssSelector(nthDestinationElementSelector(1)))
    }

    def menuIsVisible(name: String): Boolean = {
      val hiddenMenuSelector = s".js-dropdown-$name.js-dropdown-menu.animate-hide-hidden"
      val visibleMenuSelector = s".js-dropdown-$name.js-dropdown-menu"

      !pageHasElement(cssSelector(hiddenMenuSelector)) &&
      pageHasElement(cssSelector(visibleMenuSelector))
    }

    def menuContainsLink(name: String, index: Int, url: String): Boolean = {
      val dropDownSelector = s".js-dropdown-$name"
      val namedMenu = cssSelector(dropDownSelector).findElement

      namedMenu.exists{ menu =>
        val elLinkDivs = menu.underlying.findElements(By.tagName("div"))
        if (elLinkDivs.size() > 0) {
          val anchors = elLinkDivs.get(0).findElements(By.className("weekly__package"))
          if (anchors.size() > 0) {
            val anchor = anchors.get(index)
            val href = anchor.getAttribute("href")
            href.endsWith(url)
          } else {
            false
          }
        } else {
          false
        }
      }
    }

    def menuContainsTitle(name: String, index: Int, text: String): Boolean = {
      val dropDownSelector = s".js-dropdown-$name"
      val namedMenu = cssSelector(dropDownSelector).findElement

      namedMenu.exists{ menu =>
        val elSpans = menu.underlying.findElements(By.className("weekly__package__title"))
        if (elSpans.size() > 0) {
          elSpans.get(index).getText.trim.equalsIgnoreCase(text)
        } else {
          false
        }
      }
    }

    def menuContainsDescription(name: String, index: Int, text: String): Boolean = {
      val dropDownSelector = s".js-dropdown-$name"
      val namedMenu = cssSelector(dropDownSelector).findElement

      namedMenu.exists{ menu =>
        val elSpans = menu.underlying.findElements(By.className("weekly__package__description"))
        if (elSpans.size() > 0) {
          elSpans.get(index).getText.trim.equalsIgnoreCase(text)
        } else {
          false
        }
      }
    }

  }

}
