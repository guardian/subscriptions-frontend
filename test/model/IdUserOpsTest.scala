package model

import com.gu.memsub.Address
import com.gu.identity.model.{PrivateFields, PublicFields, StatusFields, User => IdUser}
import org.specs2.mutable.Specification
import IdUserOps._

class IdUserOpsTest extends Specification {
  "address" should {

    def idUser(fields: PrivateFields): IdUser =
      IdUser(
        id = "id",
        primaryEmailAddress = "email",
        publicFields = PublicFields(),
        privateFields = fields,
        statusFields = StatusFields()
      )

    val billingAddress = PrivateFields()

     val deliveryAddress = PrivateFields(
      address1 = Some("address1"),
      address2 = Some("address2"),
      address3 = Some("address3"),
      address4 = Some("address4"),
      country = Some("country"),
      postcode = Some("postcode"))

    val withBillingAddress = idUser(billingAddress)
    val withoutBillingAddress = idUser(deliveryAddress)

    "pull through billing details if present and a blank address if not" in {
      withBillingAddress.billingAddress.should_===(Address(
        "",
        "",
        "",
        "",
        "",
        ""
      ))

      withoutBillingAddress.billingAddress.should_===(Address(
        "", "", "", "", "", ""
      ))
    }

    "pull through delivery details if present and a blank address if not" in {
      withoutBillingAddress.correspondenceAddress.should_===(Address(
        deliveryAddress.address1.get,
        deliveryAddress.address2.get,
        deliveryAddress.address3.get,
        deliveryAddress.address4.get,
        deliveryAddress.postcode.get,
        deliveryAddress.country.get
      ))

      withBillingAddress.correspondenceAddress.should_===(Address(
        "", "", "", "", "", ""
      ))
    }

  }

}
