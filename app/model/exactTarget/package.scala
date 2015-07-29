package model

package object exactTarget {
  case class ExactTargetException(message: String) extends Throwable(message)
}
