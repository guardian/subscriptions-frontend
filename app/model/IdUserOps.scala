package model

import com.gu.identity.model.{User => IdUser, _}
import com.gu.memsub.Address

object IdUserOps {
  implicit class IdUserWithAddress(u: IdUser) {

    def billingAddress: Address = {
      Address(
        lineOne = u.privateFields.billingAddress1.getOrElse(""),
        lineTwo = u.privateFields.billingAddress2.getOrElse(""),
        town = u.privateFields.billingAddress3.getOrElse(""),
        countyOrState = u.privateFields.billingAddress4.getOrElse(""),
        postCode = u.privateFields.billingPostcode.getOrElse(""),
        countryName = u.privateFields.billingCountry.getOrElse("")
      )
    }

    def correspondenceAddress: Address = {
      Address(
        lineOne = u.privateFields.address1.getOrElse(""),
        lineTwo = u.privateFields.address2.getOrElse(""),
        town = u.privateFields.address3.getOrElse(""),
        countyOrState = u.privateFields.address4.getOrElse(""),
        postCode = u.privateFields.postcode.getOrElse(""),
        countryName = u.privateFields.country.getOrElse("")
      )
    }
  }
}
