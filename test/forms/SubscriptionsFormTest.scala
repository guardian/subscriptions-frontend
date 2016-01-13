package forms

import com.gu.i18n.Country
import com.gu.memsub.Address
import forms.SubscriptionsForm.{addressDataMapping, paymentFormatter, personalDataMapping}
import model._
import org.scalatest.FreeSpec
import play.api.data.Forms._

class SubscriptionsFormTest extends FreeSpec {
  val formData = Map(
    "first" -> "first",
    "last" -> "last",
    "emailValidation.email" -> "a@example.com",
    "emailValidation.confirm" -> "a@example.com",
    "receiveGnmMarketing" -> "on",
    "address.address1" -> "address1",
    "address.address2" -> "address2",
    "address.town" -> "town",
    "address.postcode" -> "postcode",
    "address.subdivision" -> "Middlesex",
    "address.country" -> Country.UK.alpha2
  )

  "PersonalDataMapping" - {
    "maps form submissions to PersonalData" in {
      assertResult(Right(PersonalData(
        first = "first",
        last = "last",
        email = "a@example.com",
        receiveGnmMarketing = true,
	address = Address("address1","address2","town", "Middlesex", "postcode", Country.UK.name)
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

  "paymentFormatter" - {
    "handle payment form data" - {
      "for direct debit" in {
        val paymentData = DirectDebitData("account", "sortcode", "holder")
        val paymentFormData = Map(
          "payment.account" -> paymentData.account,
          "payment.sortcode" -> paymentData.sortCodeValue,
          "payment.holder" -> paymentData.holder,
          "payment.type" -> DirectDebit.toKey
        )
        val mapping = single[PaymentData]("payment" -> of[PaymentData])

        assertResult(Right(paymentData))(mapping.bind(paymentFormData))
        assertResult(paymentFormData)(mapping.unbind(paymentData))
      }

      "for credit card" in {
        val paymentData = CreditCardData("token")
        val paymentFormData = Map(
          "payment.token" -> paymentData.stripeToken,
          "payment.type" -> CreditCard.toKey
        )
        val mapping = single[PaymentData]("payment" -> of[PaymentData])

        assertResult(Right(paymentData))(mapping.bind(paymentFormData))
        assertResult(Map("payment.type" -> CreditCard.toKey))(mapping.unbind(paymentData))
      }
    }
  }
}
