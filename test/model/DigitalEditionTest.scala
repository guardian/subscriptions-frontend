package model

import model.DigitalEdition.{US, UK}
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {
  private val externalSubscriptionUrl = "test.url/"

  "getRedirect" should {
    "go to the country select interstitial for UK users" in {
      DigitalEdition.getRedirect(UK, externalSubscriptionUrl) mustEqual "/digital/country"
    }

    "go straight to the subscription page for non UK users" in {
      DigitalEdition.getRedirect(US, externalSubscriptionUrl) mustEqual "test.url/?prom=DGA38&CMP=" + US.campaign
    }
  }
}
