package com.gu.acquisition.model.errors

import java.io.IOException

import okhttp3.{Request, Response}

sealed trait AnalyticsServiceError extends Throwable

object AnalyticsServiceError {

  case class BuildError(message: String) extends AnalyticsServiceError {
    override def getMessage: String = s"Acquisition submission build error: $message"
  }

  case class NetworkFailure(underlying: IOException) extends AnalyticsServiceError {
    override def getMessage: String = s"Analytics network failure: ${underlying.getMessage}"
  }

  case class ResponseUnsuccessful(request: Request, failedResponse: Response) extends AnalyticsServiceError {
    override def getMessage: String = {
      s"HTTP request failed: ${failedResponse.code}"
    }
  }

  case class KinesisError(e: Throwable) extends AnalyticsServiceError {
    override def getMessage: String = s"Kinesis put error: $e"
  }
}

