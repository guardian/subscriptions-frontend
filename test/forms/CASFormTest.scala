package forms

import com.gu.cas.{SevenDay, TokenPayload}
import com.gu.memsub.Subscription.Name
import model.CASLookup
import org.joda.time.Weeks
import org.specs2.mutable.Specification

class CASFormTest extends Specification {
  "Deserialize a CAS Lookup" in {
    val formData = Map(
      "cas.number" -> "123",
      "cas.password" -> "Las't- Name"
    )

    CASForm.lookup.bind(formData).get must_=== CASLookup(Name("123"), "Las't- Name")
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
