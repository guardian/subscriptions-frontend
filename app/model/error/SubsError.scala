package model.error

import com.gu.monitoring.SafeLogger._

trait SubsError {
  val message: String
  val request: Option[String]
  val response: Option[String]

  def toStringPretty(): String = {
    val line1  = s"\n---------------------------------------"
    val error  = s"\n${this.getClass.getSimpleName}\n"
    val strMsg = scrub"\tMessage:  \t ${message}"
    val strIn  = scrub"\n\tInput:    \t ${request}"
    val strOut = scrub"\n\tOutput:   \t ${response.getOrElse("")}"
    line1 + error + strMsg + strIn + strOut
  }
}

object SubsError {
  def toStringPretty(seqErr: SubsError): String =
    seqErr.toStringPretty().toString

  def header(seqErr: SubsError): String = {
    s"${seqErr.getClass.getSimpleName}: ${seqErr.message}"
  }
}





