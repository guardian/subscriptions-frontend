package model

import model.DigitalEdition._
import views.support.DigitalEdition._
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go straight to the checkout for all users" in {
      UK.redirect("test_button").toString mustEqual "/checkout?countryGroup=uk&startTrialButton=test_button"
      US.redirect("test_button").toString mustEqual "/checkout?countryGroup=us&startTrialButton=test_button"
      AU.redirect("test_button").toString mustEqual "/checkout?countryGroup=au&startTrialButton=test_button"
      INT.redirect("test_button").toString mustEqual "/checkout?countryGroup=int&startTrialButton=test_button"
    }
  }
}
