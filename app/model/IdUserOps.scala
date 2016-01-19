package model

import com.gu.identity.play.PrivateFields
import com.gu.identity.play.IdUser
import com.gu.memsub.Address

object IdUserOps {
  implicit class IdUserWithAddress(u: IdUser) {
    def address = {
      val pf = u.privateFields.getOrElse(PrivateFields())

      val billingAddressDefined =
        (pf.billingCountry orElse
         pf.billingAddress1 orElse
         pf.billingAddress2 orElse
         pf.billingAddress3 orElse
         pf.billingAddress4 orElse
         pf.billingPostcode).isDefined

      if (billingAddressDefined)
        Address(
          lineOne = pf.billingAddress1.getOrElse(""),
          lineTwo = pf.billingAddress2.getOrElse(""),
          town = pf.billingAddress3.getOrElse(""),
          countyOrState = pf.billingAddress4.getOrElse(""),
          postCode = pf.billingPostcode.getOrElse(""),
          countryName = pf.billingCountry.getOrElse("")
        )
      else
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
