package model.error

import scalaz.NonEmptyList

trait SubsError {
  val message: String
  val request: String
  val response: Option[String]

  def toStringPretty(): String = {
    val line1  = s"\n---------------------------------------"
    val error  = s"\n${this.getClass.getSimpleName}\n"
    val strMsg = s"\tMessage:  \t ${message}"
    val strIn  = s"\n\tInput:    \t ${request}"
    val strOut = s"\n\tOutput:   \t ${response.getOrElse("")}"
    line1 + error + strMsg + strIn + strOut
  }
}

object SubsError {
  def toStringPretty(seqErr: NonEmptyList[SubsError]): String =
    seqErr.map(_.toStringPretty()).toString()

  def header(seqErr: NonEmptyList[SubsError]): String = {
    val head = seqErr.head
    s"${head.getClass().getSimpleName}: ${seqErr.head.message}"
  }
}





