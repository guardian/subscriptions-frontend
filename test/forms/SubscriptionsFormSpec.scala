package forms

import forms.SubscriptionsForm.{personalDataMapping, addressDataMapping}
import model.{AddressData, PersonalData}
import org.scalatest.FreeSpec

class SubscriptionsFormSpec extends FreeSpec {
  val formData = Map(
    "first" -> "first",
    "last" -> "last",
    "emailValidation.email" -> "a@example.com",
    "emailValidation.confirm" -> "a@example.com",
    "receiveGnmMarketing" -> "on",
    "address.address1" -> "address1",
    "address.address2" -> "address2",
    "address.town" -> "town",
    "address.postcode" -> "postcode"
  )

  "PersonalDataMapping" - {
    "maps form submissions to PersonalData" in {
      assertResult(Right(PersonalData(
        firstName = "first",
        lastName = "last",
        email = "a@example.com",
        receiveGnmMarketing = true,
        address = AddressData(
          address1 = "address1",
          address2 = "address2",
          town = "town",
          postcode = "postcode"
        )
      )))(personalDataMapping.bind(formData))
    }

    "transforms non-empty checkbox values into a true" in {
      val data = personalDataMapping.bind(formData + ("receiveGnmMarketing" -> "true"))
      assertResult(Right(true))(data.right.map(_.receiveGnmMarketing))
    }

    "handles missing checkbox values as false" in {
      val data = personalDataMapping.bind(formData - "receiveGnmMarketing")
      assertResult(Right(false))(data.right.map(_.receiveGnmMarketing))
    }

    "validates email confirmation" in {
      val data = personalDataMapping.bind(formData + ("emailValidation.confirm" -> "other@example.com"))
      assert(data.isLeft)
    }

    Seq("first", "last").foreach { key =>
      val l = 51
      val data = personalDataMapping.bind(formData + (key -> "a" * l))
      assert(data.isLeft, s"Personal data's $key should have a max size of ${l - 1}")
    }

    Seq("address1", "address2", "town", "postcode").foreach { key =>
      val l = 256
      val data = addressDataMapping.bind(formData + (key -> "a" * l))
      assert(data.isLeft, s"Personal data's $key should have a max size of ${l - 1}")
    }

  }
}
