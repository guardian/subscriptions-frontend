package services

import akka.actor.ActorSystem
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora.ZuoraQuery
import com.gu.membership.zuora.soap.ZuoraApi
import com.gu.membership.zuora.soap.ZuoraReaders.ZuoraQueryReader
import com.gu.monitoring.ZuoraMetrics
import scala.concurrent.Future

sealed trait ZuoraFilter {
  def toFilterString: String
}

case class SimpleFilter(key: String, value: String) extends ZuoraFilter {
  override def toFilterString = s"$key='$value'"
}

case class AndFilter(clauses: (String, String)*) extends ZuoraFilter {
  override def toFilterString =
    clauses.map(p => SimpleFilter.tupled(p).toFilterString).mkString(" AND ")
}

case class OrFilter(clauses: (String, String)*) extends ZuoraFilter {
  override def toFilterString =
    clauses.map(p => SimpleFilter.tupled(p).toFilterString).mkString(" OR ")
}

class FilterableZuoraApi(apiConfig: ZuoraApiConfig, metrics: ZuoraMetrics, actorSystem:ActorSystem) extends ZuoraApi(apiConfig, metrics, actorSystem) {
  def query[T <: ZuoraQuery](where: ZuoraFilter)(implicit reader: ZuoraQueryReader[T]): Future[Seq[T]] =
    query(where.toFilterString)(reader)

  def queryOne[T <: ZuoraQuery](where: ZuoraFilter)(implicit reader: ZuoraQueryReader[T]): Future[T] =
    queryOne(where.toFilterString)(reader)
}
