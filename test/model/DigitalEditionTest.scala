package model

import model.DigitalEdition.{US, UK}
import views.support.DigitalEdition._
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  "getRedirect" should {
    "go to the country select interstitial for UK users" in {
      UK.redirect.toString mustEqual "/digital/country"
    }

    "go straight to the subscription page for non UK users" in {
      US.redirect.toString.toString mustEqual "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=dga38&CMP=FAB_3062"
    }
  }
}
