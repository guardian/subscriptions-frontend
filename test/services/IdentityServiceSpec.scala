package services

import model.PersonalData
import org.scalatest.FreeSpec
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import utils.TestIdUser._
import utils.TestPersonalData.testPersonalData
import utils.TestWSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityServiceSpec extends FreeSpec {
  class TestIdentityApiClient extends IdentityApiClient {
    override def userLookupByScGuUCookie: (String) => Future[WSResponse] = ???
    override def createGuest: (PersonalData) => Future[WSResponse] = ???
    override def updateUserDetails: (PersonalData, UserId, AuthCookie) => Future[WSResponse] = ???
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
    val validCookie = AuthCookie("valid_cookie")
    val invalidCookie = AuthCookie("invalid_cookie")

    val client = new TestIdentityApiClient {
      override val userLookupByScGuUCookie = (cookieValue: String) =>
        Future {
          val body =
            if (AuthCookie(cookieValue) == validCookie)
              Json.obj(
                "user" -> testUser
              )
            else
              JsNull

          TestWSResponse(json = body)
        }
    }

    "returns true if looking up a user by cookie is successful" in {
      makeIdentityService(client).userLookupByScGuU(validCookie).map { res =>
        assertResult(Some(testUser))(res)
      }
    }

    "returns false if looking up a user by cookie is unsuccessful" in {
      makeIdentityService(client).userLookupByScGuU(invalidCookie).map { res =>
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
        "billingPostcode" -> "AAAAAA",
        "billingCountry" -> "United Kingdom"
      ),
      "statusFields" ->
        Json.obj("receiveGnmMarketing" -> true)
    )

    assertResult(expectedJson)(
      PersonalDataJsonSerialiser.convertToUser(testPersonalData)
    )
  }
}
