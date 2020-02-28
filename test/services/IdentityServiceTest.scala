package services

import cats.Id
import com.gu.memsub.Address
import model.PersonalData
import org.scalatest.{FreeSpec, Matchers}
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import utils.TestIdUser._
import utils.TestPersonalData.testPersonalData
import utils.TestWSResponse

class IdentityServiceTest extends FreeSpec with Matchers {
  class TestIdentityApiClient extends IdentityApiClient[Id] {
    override def userLookupByCookies: (AccessCredentials.Cookies) => Id[WSResponse] = ???
    override def createGuest: (PersonalData, Option[Address]) => Id[WSResponse] = ???
    override def updateUserDetails: (PersonalData, Option[Address], AccessCredentials.Cookies) => Id[WSResponse] = ???
    override def convertGuest: (String, IdentityToken, Boolean) => Id[WSResponse] = ???
    override def userLookupByEmail: (String) => Id[WSResponse] = ???
    override def consentEmail = ???
  }

  def makeIdentityService(client: IdentityApiClient[Id]) = new IdentityService(client)

  "doesUserExists" - {
    val existingEmail = "existing@example.com"
    val client = new TestIdentityApiClient {
      override val userLookupByEmail = (email: String) =>
        {
          val (body, status) =
            if (email == existingEmail)
              (Json.obj(
                "user" -> Json.obj("id" -> "123")
              ), Status.OK)
            else
              (JsNull, Status.NOT_FOUND)

          TestWSResponse(json = body, status = status)
        }
    }

    "returns true if looking up a user by email is successful" in {
      val res = makeIdentityService(client).doesUserExist(existingEmail)
      res should be(true)
    }

    "returns false if looking up a user by email is unsuccessful" in {
      val res = makeIdentityService(client).doesUserExist("unknown@example.com")
      res should be(false)
    }
  }

  "userLookupByScGuUCookie" - {
    val validCookies = AccessCredentials.Cookies("valid_cookie")
    val invalidCookies = AccessCredentials.Cookies("invalid_cookie")

    val client = new TestIdentityApiClient {
      import com.gu.identity.model.play.WritesInstances.userWrites
      override val userLookupByCookies = (cookies: AccessCredentials.Cookies) =>
        {
          val body =
            if (cookies == validCookies)
              Json.obj(
                "user" -> testUser
              )
            else
              JsNull

          TestWSResponse(json = body)
        }
    }

    "returns true if looking up a user by cookie is successful" in {
      val res = makeIdentityService(client).userLookupByCredentials(validCookies)
        assertResult(Some(testUser))(res)
    }

    "returns false if looking up a user by cookie is unsuccessful" in {
      makeIdentityService(client).userLookupByCredentials(invalidCookies).map { res =>
        assertResult(None)(res)
      }
    }
  }

  "PersonalData serialization for the Identity service" in {
    val expectedJson = Json.obj(
      "primaryEmailAddress" -> "email@example.com",
      "privateFields" -> Json.obj(
        "firstName" -> "FirstName",
        "secondName" -> "LastName",
        "billingAddress1" -> "address1",
        "billingAddress2" -> "address2",
        "billingAddress3" -> "Town",
        "billingAddress4" -> "United Kingdom",
        "billingPostcode" -> "AAAAAA",
        "billingCountry"  -> "United Kingdom",
        "telephoneNumber" -> Json.obj(
          "countryCode" -> "44",
          "localNumber" -> "1872123456"
        ),
        "address1" -> "123 Delivery Grove",
        "address2" -> "",
        "address3" -> "Delivery upon Thames",
        "address4" -> "Deliveryshire",
        "postcode" -> "DL1 VRY",
        "country"  -> "UK"
      ),
      "statusFields" ->
        Json.obj("receiveGnmMarketing" -> true)
    )

    assertResult(expectedJson)(
      PersonalDataJsonSerialiser.convertToUser(testPersonalData, Some(Address("123 Delivery Grove", "", "Delivery upon Thames", "Deliveryshire", "DL1 VRY", "UK")))
    )
  }
}
