package services

import com.gu.identity.play.AccessCredentials
import model.PersonalData
import org.scalatest.FreeSpec
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import utils.TestIdUser._
import utils.TestPersonalData.testPersonalData
import utils.TestWSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityServiceTest extends FreeSpec {
  class TestIdentityApiClient extends IdentityApiClient {
    override def userLookupByCookies: (AccessCredentials.Cookies) => Future[WSResponse] = ???
    override def createGuest: (PersonalData) => Future[WSResponse] = ???
    override def updateUserDetails: (PersonalData, AccessCredentials.Cookies) => Future[WSResponse] = ???
    override def convertGuest: (String, IdentityToken) => Future[WSResponse] = ???
    override def userLookupByEmail: (String) => Future[WSResponse] = ???
  }

  def makeIdentityService(client: IdentityApiClient) = new IdentityService(client)

  "doesUserExists" - {
    val existingEmail = "existing@example.com"
    val client = new TestIdentityApiClient {
      override val userLookupByEmail = (email: String) =>
        Future {
          val body =
            if (email == existingEmail)
              Json.obj(
                "user" -> Json.obj("id" -> "123")
              )
            else
              JsNull

          TestWSResponse(json = body)
        }
    }

    "returns true if looking up a user by email is successful" in {
      makeIdentityService(client).doesUserExist(existingEmail).map { res =>
        assert(res)
      }
    }

    "returns false if looking up a user by email is unsuccessful" in {
      makeIdentityService(client).doesUserExist("unknown@example.com").map { res =>
        assert(!res)
      }
    }
  }

  "userLookupByScGuUCookie" - {
    val validCookies = AccessCredentials.Cookies("valid_cookie")
    val invalidCookies = AccessCredentials.Cookies("invalid_cookie")

    val client = new TestIdentityApiClient {
      override val userLookupByCookies = (cookies: AccessCredentials.Cookies) =>
        Future {
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
      makeIdentityService(client).userLookupByCredentials(validCookies).map { res =>
        assertResult(Some(testUser))(res)
      }
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
      "publicFields" -> Json.obj(
        "displayName" -> "FirstName LastName"
      ),
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
        )
      ),
      "statusFields" ->
        Json.obj("receiveGnmMarketing" -> true)
    )

    assertResult(expectedJson)(
      PersonalDataJsonSerialiser.convertToUser(testPersonalData)
    )
  }
}
