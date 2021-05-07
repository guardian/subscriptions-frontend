package com.gu.acquisition.services

import java.io.IOException

import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.model.errors.AnalyticsServiceError.{BuildError, NetworkFailure, ResponseUnsuccessful}
import com.gu.acquisition.services.HttpAnalyticsService.RequestData
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import okhttp3._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

private [acquisition] abstract class HttpAnalyticsService(implicit client: OkHttpClient) extends AnalyticsService {

  protected def buildRequest(submission: AcquisitionSubmission)(implicit ec: ExecutionContext): EitherT[Future, BuildError, RequestData]

  private def executeRequest(data: RequestData): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission] = {

    val p = Promise[Either[AnalyticsServiceError, AcquisitionSubmission]]

    client.newCall(data.request).enqueue(new Callback {

      override def onFailure(call: Call, e: IOException): Unit =
        p.success(Left(NetworkFailure(e)))

      private def close(response: Response): Unit =
        try {
          response.close()
        } catch {
          case NonFatal(_) =>
        }

      override def onResponse(call: Call, response: Response): Unit =
        try {
          if (response.isSuccessful) p.success(Right(data.submission))
          else p.success(Left(ResponseUnsuccessful(data.request, response)))
        } finally {
          close(response)
        }
    })

    EitherT(p.future)
  }

  def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission] = {

    import AcquisitionSubmissionBuilder.ops._
    import cats.instances.future._

    for {
      submission <- EitherT.fromEither(a.asAcquisitionSubmission)
      request <- buildRequest(submission)
      result <- executeRequest(request)
    } yield result
  }

}

object HttpAnalyticsService {

  private[services] case class RequestData(request: Request, submission: AcquisitionSubmission)
}
