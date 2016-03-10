package model

trait SubsError {
  val message: String
  val request: String
  val response: Option[String]

  def toStringPretty(): String = {
    val line1  = s"\n----------------------------------------"
    val error  = s"\nError: ${this.getClass.getSimpleName}\n"
    val line2   = s"----------------------------------------\n"
    val strMsg = s"\n\tMessage:  \n\t\t ${message}"
    val strIn  = s"\n\tInput:    \n\t\t ${request}"
    val strOut = s"\n\tOutput:   \n\t\t ${response.getOrElse("")}"
    line1 + error + line2 + strMsg + strIn + strOut
  }
}
