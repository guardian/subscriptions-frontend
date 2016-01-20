package forms

import model.CASLookup
import org.specs2.mutable.Specification

class CASFormTest extends Specification {
  val formData = Map(
    "cas.number" -> "123",
    "cas.lastName" -> "Last Name"
  )

  "Deserialize a CAS Lookup" in {
    CASForm().bind(formData).get must_=== CASLookup("123", "Last Name", None)
    CASForm().bind(formData + ("cas.postcode" -> "456")).get must_=== CASLookup("123", "Last Name", Some("456"))
  }
}
