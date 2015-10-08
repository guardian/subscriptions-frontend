package model

import model.DigitalEdition.{UrlConfig, US, UK}
import play.api.test.PlaySpecification

class DigitalEditionTest extends PlaySpecification {

  private object configStub extends UrlConfig {
    val externalSubscriptionUrl = "test.url/"
  }

  "getRedirect" should {
    "go to the country select interstitial for UK users" in {
      DigitalEdition.getRedirect(UK)(configStub) mustEqual "/digital/country"
    }

    "go straight to the subscription page for non UK users" in {
      DigitalEdition.getRedirect(US)(configStub) mustEqual "test.url/?prom=DGA38&CMP=" + US.campaign
    }
  }
}
