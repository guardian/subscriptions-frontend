package forms

import model.CASLookup
import com.gu.cas.{SevenDay, TokenPayload}
import org.joda.time.{Weeks, Days}
import org.specs2.mutable.Specification

class CASFormTest extends Specification {
  "Deserialize a CAS Lookup" in {
    val formData = Map(
      "cas.number" -> "123",
      "cas.lastName" -> "Last Name"
    )

    CASForm.lookup.bind(formData).get must_=== CASLookup("123", "Last Name", None)
    CASForm.lookup.bind(formData + ("cas.postcode" -> "456")).get must_=== CASLookup("123", "Last Name", Some("456"))
  }

  "Deserialize a token payload" in {
    val formData = Map(
      "cas.creationDateOffset" -> "1",
      "cas.period" -> "5",
      "cas.subscriptionCode" -> "SevenDay"
    )

    CASForm.emergencyToken.bind(formData).get must_=== TokenPayload(Weeks.weeks(5), SevenDay)
  }
}
