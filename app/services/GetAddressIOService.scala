package services

import com.gu.memsub.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import com.gu.okhttp.RequestRunners
import com.typesafe.scalalogging.LazyLogging
import monitoring.Metrics
import services.GetAddressIOModel.{FindAddressResult, FindAddressResultError}

import scala.concurrent.{ExecutionContext, Future}

object GetAddressIOModel {
  case class FindAddressResult(Latitude: Float, Longitude: Float, Addresses: Seq[String])
  case class FindAddressResultError(Message: String) extends RuntimeException(s"$Message")
}

object GetAddressIOMetrics extends Metrics with StatusMetrics {
  override val service: String = "GetAddressIO"
}

class GetAddressIOService()(implicit executionContext: ExecutionContext) extends WebServiceHelper[FindAddressResult, FindAddressResultError] with LazyLogging {
  import configuration.Config
  import play.api.libs.functional.syntax._
  import play.api.libs.json.{JsPath, Reads}

  implicit val errorReads: Reads[FindAddressResultError] = (JsPath \ "Message").read[String].map(FindAddressResultError)
  implicit val farReads: Reads[FindAddressResult] = (
    (JsPath \ "Latitude").read[Float] and (JsPath \ "Longitude").read[Float] and (JsPath \ "Addresses").read[Seq[String]]
    )(FindAddressResult)

  override val wsUrl: String = Config.getAddressIOApiUrl
  override val httpClient = RequestRunners.loggingRunner(GetAddressIOMetrics)
  def find(postcode: String): Future[FindAddressResult] = get[FindAddressResult](postcode, "api-key" -> Config.getAddressIOApiKey).transform(x => x, y => y)
}
