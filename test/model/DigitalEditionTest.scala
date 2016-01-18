package model

import model.DigitalEdition.{AU, US, UK}
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go straight to the UK checkout" in {
      DigitalEdition.getRedirect(UK).toString mustEqual "/checkout?countryGroup=uk"
    }
    "go straight to the US checkout" in {
      DigitalEdition.getRedirect(US).toString mustEqual "/checkout?countryGroup=us"
    }
    "go straight to the AU checkout" in {
      DigitalEdition.getRedirect(AU).toString mustEqual "/checkout?countryGroup=au"
    }
  }
}
