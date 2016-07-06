package services
import com.gu.i18n.Title
import com.gu.memsub.Address
import model.PersonalData
import org.specs2.mutable.Specification
import com.gu.salesforce.ContactDeserializer.Keys._

import scalaz.syntax.std.option._
import play.api.libs.json.{JsString, Json}

class SalesforceServiceTest extends Specification {

  "Salesforce contact serialiser" should {

    val bloggsResidence = Address("Flat 123", "123 Fake Street", "Faketown", "Kent", "FT1 0HF", "UK")
    val data = PersonalData("Joe", "Bloggs", "joebloggs@example,com", receiveGnmMarketing = true, bloggsResidence, Some("1234"), Title.Prof.some)

    "Serialise your good old fashioned basic fields" in {
      SalesforceService.createSalesforceUserData(personalData = data, None) mustEqual Json.obj(
        EMAIL -> data.email,
        FIRST_NAME -> data.first,
        LAST_NAME -> data.last,
        BILLING_STREET -> data.address.line,
        BILLING_CITY -> data.address.town,
        BILLING_POSTCODE -> data.address.postCode,
        BILLING_COUNTRY -> data.address.countryName,
        ALLOW_GU_RELATED_MAIL -> data.receiveGnmMarketing,
        BILLING_STATE -> data.address.countyOrState
      )
    }

    "Include your mailing address if you supply one" in {
      val delivery = Address("Flat 456", "123 Delivery Grove", "Deliverytown", "Surrey", "DL1 ABC", "UK")
      val salesforceInfo = SalesforceService.createSalesforceUserData(data, delivery.some)
      (salesforceInfo \ MAILING_STREET).get mustEqual JsString(delivery.line)
      (salesforceInfo \ MAILING_CITY).get mustEqual JsString(delivery.town)
      (salesforceInfo \ MAILING_POSTCODE).get mustEqual JsString(delivery.postCode)
      (salesforceInfo \ MAILING_COUNTRY).get mustEqual JsString(delivery.countryName)
      (salesforceInfo \ MAILING_STATE).get mustEqual JsString(delivery.countyOrState)
    }
  }
}
