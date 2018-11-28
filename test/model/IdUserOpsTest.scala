package model

import com.gu.memsub.Address
import com.gu.identity.play.{PublicFields, PrivateFields, IdUser}
import org.specs2.mutable.Specification
import IdUserOps._

class IdUserOpsTest extends Specification {
  "address" should {

    def idUser(fields: PrivateFields): IdUser =
      IdUser(
        id = "id",
        primaryEmailAddress = "email",
        publicFields = PublicFields(None),
        privateFields = Some(fields),
        statusFields = None)

    val billingAddress = PrivateFields(
      billingAddress1 = Some("billingAddress1"),
      billingAddress2 = Some("billingAddress2"),
      billingAddress3 = Some("billingAddress3"),
      billingAddress4 = Some("billingAddress4"),
      billingCountry = Some("billingCountry"),
      billingPostcode = Some("billingPostcode"))

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
        billingAddress.billingAddress1.get,
        billingAddress.billingAddress2.get,
        billingAddress.billingAddress3.get,
        billingAddress.billingAddress4.get,
        billingAddress.billingPostcode.get,
        billingAddress.billingCountry.get
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
