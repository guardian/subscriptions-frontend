package views.support

import com.gu.i18n.{Country, CountryGroup}
import org.specs2.mutable.Specification

class DetermineCountryGroupTest extends Specification {

  "DetermineCountryGroup fromHint" should {

    "return None when hint is nonsense" in {
      val gottenCountryGroup = DetermineCountryGroup.fromHint("nonsense")
      gottenCountryGroup must_=== None
    }

    "return a Europe countryGroup with Germany defaultCountry when 'de' is provided." in {
      val gottenCountryGroup = DetermineCountryGroup.fromHint("de")

      assert(gottenCountryGroup.isDefined)
      gottenCountryGroup.get.id must_=== CountryGroup.Europe.id
      assert(gottenCountryGroup.get.defaultCountry.isDefined)
      gottenCountryGroup.get.defaultCountry.get must_=== Country("DE","Germany", Nil)
    }

    "return a Europe countryGroup with Germany defaultCountry when 'DE' is provided." in {
      val gottenCountryGroup = DetermineCountryGroup.fromHint("DE")

      assert(gottenCountryGroup.isDefined)
      gottenCountryGroup.get.id must_=== CountryGroup.Europe.id
      assert(gottenCountryGroup.get.defaultCountry.isDefined)
      gottenCountryGroup.get.defaultCountry.get must_=== Country("DE","Germany", Nil)
    }

    "return a Europe countryGroup with Ireland defaultCountry when 'ie' is provided." in {
      val gottenCountryGroup = DetermineCountryGroup.fromHint("ie")

      assert(gottenCountryGroup.isDefined)
      gottenCountryGroup.get.id must_=== CountryGroup.Europe.id
      assert(gottenCountryGroup.get.defaultCountry.isDefined)
      gottenCountryGroup.get.defaultCountry.get must_=== Country.Ireland
    }

    "return a Europe countryGroup with Ireland defaultCountry when 'IE' is provided." in {
      val gottenCountryGroup = DetermineCountryGroup.fromHint("IE")

      assert(gottenCountryGroup.isDefined)
      gottenCountryGroup.get.id must_=== CountryGroup.Europe.id
      assert(gottenCountryGroup.get.defaultCountry.isDefined)
      gottenCountryGroup.get.defaultCountry.get must_=== Country.Ireland
    }
  }
}
