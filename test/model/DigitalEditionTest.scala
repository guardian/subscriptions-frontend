package model

import model.DigitalEdition._
import views.support.DigitalEdition._
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go straight to the checkout for all users" in {
      UK.redirect.toString mustEqual "/checkout?countryGroup=uk"
      US.redirect.toString mustEqual "/checkout?countryGroup=us"
      AU.redirect.toString mustEqual "/checkout?countryGroup=au"
      INT.redirect.toString mustEqual "/checkout?countryGroup=int"
    }
  }
}
