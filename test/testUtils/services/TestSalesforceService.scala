package testUtils.services

import com.gu.membership.salesforce.{BasicMember, MemberId}
import model.PersonalData
import services.{IdUser, SalesforceService}

import scala.concurrent.Future

object TestSalesforceService {

  val futureUser = Future.successful(BasicMember("", ""))
  val futureFailure = Future.failed(new RuntimeException)

  def apply(_createSFUser: Future[MemberId] = futureUser) = new SalesforceService {
    override def createSFUser(personalData: PersonalData, idUser: IdUser): Future[MemberId] = _createSFUser
  }
}
