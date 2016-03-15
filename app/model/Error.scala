package model

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
  def toStringPretty(seqErr: Seq[SubsError]): String = {
    seqErr.head.message + ":\n" + seqErr.map(_.toStringPretty()).toString()
  }
}
