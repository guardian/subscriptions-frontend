package model

import com.gu.identity.play.{IdUser, PrivateFields}
import com.gu.memsub.Address

object IdUserOps {
  implicit class IdUserWithAddress(u: IdUser) {

    def billingAddress: Address = {
      val pf = u.privateFields.getOrElse(PrivateFields())
        Address(
          lineOne = pf.billingAddress1.getOrElse(""),
          lineTwo = pf.billingAddress2.getOrElse(""),
          town = pf.billingAddress3.getOrElse(""),
          countyOrState = pf.billingAddress4.getOrElse(""),
          postCode = pf.billingPostcode.getOrElse(""),
          countryName = pf.billingCountry.getOrElse("")
        )
    }

    def address: Address = {
      val pf = u.privateFields.getOrElse(PrivateFields())
        Address(
          lineOne = pf.address1.getOrElse(""),
          lineTwo = pf.address2.getOrElse(""),
          town = pf.address3.getOrElse(""),
          countyOrState = pf.address4.getOrElse(""),
          postCode = pf.postcode.getOrElse(""),
          countryName = pf.country.getOrElse("")
        )
    }
  }
}
