package testUtils.services

import model.PersonalData
import services.{IdUser, IdentityApiClient, IdentityService}

import scala.concurrent.Future

object TestIdentityService {
  private val fakeIdentityApiClient = new IdentityApiClient {
    override def createGuest = ???
    override def userLookupByScGuUCookie = ???
    override def userLookupByEmail = ???
  }

  val futureUser = Future.successful(Some(IdUser("123456")))
  val futureNone: Future[Option[IdUser]] = Future.successful(None)

  def apply(_userLookupByScGuU: Future[Option[IdUser]] = futureNone,
            _userLookupByEmail: Future[Option[IdUser]] = futureNone,
            _registerGuest: Future[Option[IdUser]] = futureNone) = new IdentityService(fakeIdentityApiClient) {
    override def userLookupByScGuU(cookieValue: String): Future[Option[IdUser]] = _userLookupByScGuU
    override def userLookupByEmail(email: String): Future[Option[IdUser]] = _userLookupByEmail
    override def registerGuest(personalData: PersonalData): Future[Option[IdUser]] = _registerGuest
  }
}
