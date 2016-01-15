package model

import model.DigitalEdition.{US, UK}
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go straight to the checkout" in {
      DigitalEdition.getRedirect(UK).toString mustEqual "/checkout"
    }
  }
}
