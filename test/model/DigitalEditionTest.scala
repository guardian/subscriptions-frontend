package model

import model.DigitalEdition._
import views.support.DigitalEdition._
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go straight to the checkout for all users" in {
      UK.redirect.toString mustEqual "/checkout/digitalpack-digitalpackmonthly?countryGroup=uk"
      US.redirect.toString mustEqual "/checkout/digitalpack-digitalpackmonthly?countryGroup=us"
      AU.redirect.toString mustEqual "/checkout/digitalpack-digitalpackmonthly?countryGroup=au"
      INT.redirect.toString mustEqual "/checkout/digitalpack-digitalpackmonthly?countryGroup=int"
    }
  }
}
