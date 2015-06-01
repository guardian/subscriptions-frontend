package services

import akka.agent.Agent
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce.{Authentication, MemberId, MemberRepository, Scalaforce}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsObject, Json}
import services.CheckoutService._
import services.IdentityService.IdUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, Future}
import scala.util.Left

object SalesforceService extends LazyLogging {

  def createSFUser(personalData: PersonalData, idUser: IdUser): Future[Either[CheckoutException, MemberId]] = {
    val data: JsObject = createSalesforceUserData(personalData)
    val x = SalesforceRepo.upsert(idUser.id, data)
      .map(Right(_)).recoverWith {
        case e: Throwable =>
          logger.error("Could not create saleforce user", e)
          Future.successful(Left(SalesforceUserNotCreated))
      }
    x
  }

  private def createSalesforceUserData(personalData: PersonalData): JsObject = {
    Seq(Json.obj(
      Keys.EMAIL -> personalData.email,
      Keys.FIRST_NAME -> personalData.firstName,
      Keys.LAST_NAME -> personalData.lastName,
      Keys.MAILING_STREET -> personalData.address.street,
      Keys.MAILING_CITY -> personalData.address.town,
      //      Keys.MAILING_STATE -> ???,
      Keys.MAILING_POSTCODE -> personalData.address.postcode,
      Keys.MAILING_COUNTRY -> "United Kingdom",
      Keys.ALLOW_MEMBERSHIP_MAIL -> true
    )) ++ Map(
      Keys.ALLOW_THIRD_PARTY_EMAIL -> Some(true),
      Keys.ALLOW_GU_RELATED_MAIL -> Some(true)
    ).collect { case (k, Some(v)) => Json.obj(k -> v) }
  }.reduce(_ ++ _)

}

object SalesforceRepo extends MemberRepository {
  override val salesforce = new Scalaforce {
    override val consumerKey = Config.Salesforce.consumerKey
    override val apiUsername = Config.Salesforce.apiUsername
    override val consumerSecret = Config.Salesforce.consumerSecret
    override val apiToken = Config.Salesforce.apiToken
    override val apiPassword = Config.Salesforce.apiPassword
    override val application = "subscriptions-frontend"
    override val apiURL = Config.Salesforce.apiURL.toString
    override val stage = Config.Salesforce.envName

    val authTask = ScheduledTask("", Authentication("", ""), 0.seconds, 30.minutes)(getAuthentication)

    def authentication: Authentication = authTask.get()
  }
}

trait ScheduledTask[T] {
  import play.api.Play.current

  val initialValue: T
  val initialDelay: FiniteDuration
  val interval: FiniteDuration

  val name = getClass.getSimpleName

  private implicit val system = Akka.system
  lazy val agent = Agent[T](initialValue)

  def refresh(): Future[T]

  def start() {
    Logger.debug(s"Starting $name scheduled task")
    system.scheduler.schedule(initialDelay, interval) {
      agent.sendOff { _ =>
        Logger.debug(s"Refreshing $name scheduled task")
        Await.result(refresh(), 25.seconds)
      }
    }
  }

  def get() = agent.get()
}

object ScheduledTask {
  def apply[T](taskName: String, initValue: T, initDelay: FiniteDuration, intervalPeriod: FiniteDuration)(refresher: => Future[T]) =
    new ScheduledTask[T] {
      val initialValue = initValue
      val initialDelay = initDelay
      val interval = intervalPeriod

      override val name = taskName

      def refresh(): Future[T] = refresher
    }
}
