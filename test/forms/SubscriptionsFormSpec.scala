package forms

import com.gu.membership.zuora.{Address, Countries}
import forms.SubscriptionsForm.{addressDataMapping, personalDataMapping}
import model.PersonalData
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
        address = Address("address1","address2","town", "United Kingdom", "postcode", Countries.UK)
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
      s"checks the size of the $key field" in {
        assert(data.isLeft, s"Personal data's $key should have a max size of ${l - 1}")
      }
    }

    Seq("address1", "address2", "town", "postcode").foreach { key =>
      val l = 256
      val data = addressDataMapping.bind(formData + (key -> "a" * l))
      s"checks the size of the $key field" in {
        assert(data.isLeft, s"Personal data's $key should have a max size of ${l - 1}")
      }
    }

    "checks the size of the email field" in {
      val l = 229 // remove the domain length from 241
      val email = ("a" * l) + "@example.com"
      val data = addressDataMapping.bind(
        formData + ("emailValidation.email" -> email) + ("emailValidation.confirm" -> email)
      )

      assert(data.isLeft, s"Email should have a max size of 240")
    }
  }
}
