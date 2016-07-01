package services

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.Address
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import model.PersonalData
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future

trait SalesforceService extends LazyLogging {
  def repo: SimpleContactRepository

  def createOrUpdateUser(personalData: PersonalData, deliveryAddress: Option[Address], userId: Option[IdMinimalUser]): Future[ContactId]

  def createSalesforceUserData(personalData: PersonalData, deliveryAddress: Option[Address]): JsObject = Json.obj(
      Keys.EMAIL -> personalData.email,
      Keys.FIRST_NAME -> personalData.first,
      Keys.LAST_NAME -> personalData.last,
      Keys.BILLING_STREET -> personalData.address.line,
      Keys.BILLING_CITY -> personalData.address.town,
      Keys.BILLING_POSTCODE -> personalData.address.postCode,
      Keys.BILLING_COUNTRY -> personalData.address.countryName,
      Keys.ALLOW_GU_RELATED_MAIL -> personalData.receiveGnmMarketing
  ) ++ deliveryAddress.fold(Json.obj())(addr => Json.obj(
      Keys.MAILING_STREET -> personalData.address.line,
      Keys.MAILING_CITY -> personalData.address.town,
      Keys.MAILING_POSTCODE -> personalData.address.postCode,
      Keys.MAILING_COUNTRY -> personalData.address.countryName,
      Keys.ALLOW_GU_RELATED_MAIL -> personalData.receiveGnmMarketing
  ))
}

class SalesforceServiceImp(val repo: SimpleContactRepository) extends SalesforceService {
  override def createOrUpdateUser(personalData: PersonalData, deliveryAddress: Option[Address], userId: Option[IdMinimalUser]): Future[ContactId] =
    repo.upsert(userId.map(_.id), createSalesforceUserData(personalData, deliveryAddress))
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
