package services

import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.gu.memsub.util.{ScheduledTaskConfig, ScheduledTask, FutureSupplier}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsObject, Json}
import scala.concurrent.Future
import scala.concurrent.duration._

trait SalesforceService extends LazyLogging {
  def repo: SalesforceRepo

  def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[ContactId]

  def createSalesforceUserData(personalData: PersonalData): JsObject = Json.obj(
      Keys.EMAIL -> personalData.email,
      Keys.FIRST_NAME -> personalData.first,
      Keys.LAST_NAME -> personalData.last,
      Keys.MAILING_STREET -> personalData.address.line,
      Keys.MAILING_CITY -> personalData.address.town,
      Keys.MAILING_POSTCODE -> personalData.address.postCode,
      Keys.MAILING_COUNTRY -> personalData.address.countryName,
      Keys.ALLOW_GU_RELATED_MAIL -> personalData.receiveGnmMarketing)

}

class SalesforceServiceImp(val repo: SalesforceRepo) extends SalesforceService {
  override def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[ContactId] =
    repo.upsert(userId.id, createSalesforceUserData(personalData))
}

class SalesforceRepo(salesforceConfig: SalesforceConfig) extends ContactRepository {
  override val salesforce = new Scalaforce {
    override val consumerKey = salesforceConfig.consumerKey
    override val apiUsername = salesforceConfig.apiUsername
    override val consumerSecret = salesforceConfig.consumerSecret
    override val apiToken = salesforceConfig.apiToken
    override val apiPassword = salesforceConfig.apiPassword
    override val application = Config.appName
    override val apiURL =salesforceConfig.apiURL.toString()
    override val stage = salesforceConfig.envName

    override val authSupplier: FutureSupplier[Authentication] = new FutureSupplier[Authentication](getAuthentication)

    implicit private val actorSystem = Akka.system

    private val getAuthenticationTaskConfig =
      ScheduledTaskConfig[Authentication](
        "SalesforceRepo - getAuthentication", Authentication("", ""), 30.minutes, 30.minutes)

    private val getAuthenticationTask = ScheduledTask[Authentication](getAuthenticationTaskConfig){ authSupplier.refresh() }
    getAuthenticationTask.start()
  }
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
