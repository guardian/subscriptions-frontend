package services

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.NormalisedTelephoneNumber
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import model.{PaperData, PersonalData}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future

trait SalesforceService extends LazyLogging {
  def repo: SimpleContactRepository
  def createOrUpdateUser(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[ContactId]
  def isAuthenticated: Boolean
}

class SalesforceServiceImp(val repo: SimpleContactRepository) extends SalesforceService {
  override def createOrUpdateUser(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[ContactId] =
    repo.upsert(userId.map(_.id), SalesforceService.createSalesforceUserData(personalData, paperData))

  override def isAuthenticated = repo.salesforce.isAuthenticated
}

object SalesforceService {
  def createSalesforceUserData(personalData: PersonalData, paperData: Option[PaperData]): JsObject = Json.obj(
    Keys.EMAIL -> personalData.email,
    Keys.FIRST_NAME -> personalData.first,
    Keys.LAST_NAME -> personalData.last,
    Keys.BILLING_STREET -> personalData.address.line,
    Keys.BILLING_CITY -> personalData.address.town,
    Keys.BILLING_POSTCODE -> personalData.address.postCode,
    Keys.BILLING_COUNTRY -> personalData.address.country.fold(personalData.address.countryName)(_.name),
    Keys.BILLING_STATE -> personalData.address.countyOrState,
    Keys.ALLOW_GU_RELATED_MAIL -> personalData.receiveGnmMarketing
  ) ++ personalData.title.fold(Json.obj())(title => Json.obj(
    Keys.TITLE -> title.title
  )) ++ paperData.map(_.deliveryAddress).fold(Json.obj())(addr => Json.obj(
    Keys.MAILING_STREET -> addr.line,
    Keys.MAILING_CITY -> addr.town,
    Keys.MAILING_POSTCODE -> addr.postCode,
    Keys.MAILING_STATE -> addr.countyOrState,
    Keys.MAILING_COUNTRY -> addr.country.fold(addr.countryName)(_.name)
  ) ++ paperData.flatMap(_.deliveryInstructions).fold(Json.obj())(instrs => Json.obj(
    Keys.DELIVERY_INSTRUCTIONS -> instrs
  ))) ++ NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber, personalData.address.country).fold(Json.obj())(phone => Json.obj(Keys.TELEPHONE -> phone.format))
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
