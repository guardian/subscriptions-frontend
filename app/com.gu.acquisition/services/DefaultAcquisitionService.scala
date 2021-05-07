package com.gu.acquisition.services

import cats.data.EitherT
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3.{HttpUrl, OkHttpClient}

import scala.concurrent.ExecutionContext

sealed trait DefaultAcquisitionServiceConfig {
  val kinesisStreamName: String
  val ophanEndpoint: Option[HttpUrl]
}

//Credentials provider is only required by the kinesis client when running in ec2 or locally
case class Ec2OrLocalConfig(credentialsProvider: AWSCredentialsProviderChain, kinesisStreamName: String, ophanEndpoint: Option[HttpUrl] = None) extends DefaultAcquisitionServiceConfig

case class LambdaConfig(kinesisStreamName: String, ophanEndpoint: Option[HttpUrl] = None) extends DefaultAcquisitionServiceConfig

class DefaultAcquisitionService(config: DefaultAcquisitionServiceConfig)(implicit client: OkHttpClient) extends AcquisitionService {
  private val ophanService = new OphanService(config.ophanEndpoint)
  private val gAService = new GAService()
  private val kinesisService = new KinesisService(config)

  override def submit[A: AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext) = {
    val ov = ophanService.submit(a).value
    val gv = gAService.submit(a).value
    val kv = kinesisService.submit(a).value
    val result = for {
      o <- ov
      g <- gv
      k <- kv
    } yield mergeEithers(List(o, g, k))
    EitherT(result)
  }

  // Return the AcquisitionSubmission only if there are no errors, otherwise the full List[AnalyticsServiceError]
  def mergeEithers(eithers: List[Either[AnalyticsServiceError, AcquisitionSubmission]]): Either[List[AnalyticsServiceError], AcquisitionSubmission] = {
    val errors: List[AnalyticsServiceError] = eithers.flatMap(_.swap.toOption)
    val submission: Option[AcquisitionSubmission] = eithers.collectFirst { case Right(s) => s }

    (errors, submission) match {
      case (Nil, Some(s)) => Right(s)
      case _ => Left(errors)
    }
  }
}
