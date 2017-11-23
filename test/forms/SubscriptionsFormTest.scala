package forms

import com.gu.i18n.Country
import com.gu.i18n.Currency.GBP
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.Address
import forms.SubscriptionsForm._
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
    "address.country" -> Country.UK.alpha2,
    "promoCode" -> "promo-code"
  )

  "DeliveryAddressMapping" - {

    val withDelivery = Map(
      "delivery.address1" -> "DELIVERY ADDR 1",
      "delivery.address2" -> "DELIVERY ADDR 2",
      "delivery.town" -> "DELIVERY TOWN",
      "delivery.postcode" -> "delivery postcode",
      "delivery.subdivision" -> "DELIVERY SUBDIVISION",
      "delivery.country" -> Country.UK.alpha2
    ) ++ formData

    "Reads from delivery if available" in {
      assertResult(Right(Address(
        lineOne = "DELIVERY ADDR 1",
        lineTwo = "DELIVERY ADDR 2",
        town = "DELIVERY TOWN",
        countyOrState = "DELIVERY SUBDIVISION",
        postCode = "DELIVERY POSTCODE",
        countryName = Country.UK.name
      )))(addressWithFallback(fallbackKey = "address").bind("delivery", withDelivery))
    }

    "Falls back to the fallback if it fails to read delivery" in {
      assertResult(Right(Address(
        lineOne = "address1",
        lineTwo = "address2",
        town = "town",
        countyOrState = "Middlesex",
        postCode = "POSTCODE",
        countryName = Country.UK.name
      )))(addressWithFallback(fallbackKey = "address").bind("delivery", withDelivery.drop(1)))
    }

  }

  "PersonalDataMapping" - {
    "maps form submissions to PersonalData" in {
      assertResult(Right(PersonalData(
        first = "first",
        last = "last",
        email = "a@example.com",
        receiveGnmMarketing = true,
	address = Address("address1","address2","town", "Middlesex", "POSTCODE", Country.UK.name),
        telephoneNumber = None
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

  "PromoCodeFormatter" - {
    "handles promo code" in {
      val mapping = single[PromoCode]("promoCode" -> of[PromoCode])

      assert(mapping.bind(Map("promoCode" -> "")).isLeft)
      assert(mapping.bind(Map("blah" -> "...")).isLeft)
      assertResult(Right(PromoCode("code")))(mapping.bind(Map("promoCode" -> "code")))
    }
  }

  "OphanDataMapping" - {
    "maps form submissions to OphanData" in {
      assertResult(Right(OphanData(
        pageViewId = Some("blah"),
        visitId = Some("blah"),
        browserId = Some("blah")
      )))(ophanDataMapping.bind(Map(
        "pageViewId" -> "blah",
        "visitId" -> "blah",
        "browserId" -> "blah"
      ) ++ formData))
    }

    "handles missing ophan data" in {
      assertResult(Right(OphanData(
        pageViewId = None,
        visitId = Some("blah"),
        browserId = Some("blah")
      )))(ophanDataMapping.bind(Map(
        "visitId" -> "blah",
        "browserId" -> "blah"
      ) ++ formData))
    }

    "handles all ophan data missing" in {
      assertResult(Right(OphanData(
        pageViewId = None,
        visitId = None,
        browserId = None
      )))(ophanDataMapping.bind(formData))
    }
  }

  "Checkout form" - {
    val formData = Map(
      "csrfToken" -> "0e8b61a3653c59b798a2755f7d342268cdb72d1c-1511366056806-3497ae30a15909d24dc651e7",
      "ratePlanId" -> "2c92c0f84bbfec8b014bc655f4852d9d",
      "promoCode" -> "promo",
      "personal.first" -> "first",
      "personal.last" -> "last",
      "personal.emailValidation.email" -> "a@example.com",
      "personal.emailValidation.confirm" -> "a@example.com",
      "currency" -> GBP.iso,
      "personal.address.address1" -> "address1",
      "personal.address.address2" -> "address2",
      "personal.address.town" -> "town",
      "personal.address.country" -> Country.UK.alpha2,
      "personal.address.subdivision" -> "subdivision",
      "personal.address.postcode" -> "POSTCODE",
      "payment.type" -> "direct-debit",
      "payment.token" -> "token",
      "payment.sortcode" -> "sortcode",
      "payment.account" -> "account",
      "payment.holder" -> "holder",
      "ophan.browserId" -> "bwid",
      "ophan.visitId" -> "vid",
      "ophan.pageViewId" -> "pvid"
    )

    val address = Address(
      lineOne = "address1",
      lineTwo = "address2",
      town = "town",
      countyOrState = "subdivision",
      postCode = "POSTCODE",
      countryName = Country.UK.name
    )

    val personalData = PersonalData(
      first = "first",
      last = "last",
      email = "a@example.com",
      receiveGnmMarketing = false,
      address = address,
      telephoneNumber = None
    )

    val ophanData = OphanData(Some("pvid"), Some("vid"), Some("bwid"))

    val paymentData = DirectDebitData("account", "sortcode", "holder")

    val subscriptionData = SubscriptionData(
      personalData,
      paymentData,
      Some(PromoCode("promo")),
      GBP,
      ophanData
    )

    "handles all sections" in {
      val boundForm = subsForm.bind(formData)
      assertResult(Some(subscriptionData))(boundForm.value)
    }
  }
}
