package model

import com.gu.identity.model.{User => IdUser, _}
import com.gu.memsub.Address

object IdUserOps {
  implicit class IdUserWithAddress(u: IdUser) {

    def billingAddress: Address = {
      Address(
        lineOne = "",
        lineTwo = "",
        town = "",
        countyOrState = "",
        postCode = "",
        countryName = ""
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
