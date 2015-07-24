package model

package object exactTarget {
  trait DataExtensionColumn

  case object `SubscriberKey` extends DataExtensionColumn
  case object `EmailAddress` extends DataExtensionColumn
  case object `Subscription term` extends DataExtensionColumn
  case object `Payment amount` extends DataExtensionColumn
  case object `Default payment method` extends DataExtensionColumn
  case object `First Name` extends DataExtensionColumn
  case object `Last Name` extends DataExtensionColumn
  case object `Address 1` extends DataExtensionColumn
  case object `Address 2` extends DataExtensionColumn
  case object `City` extends DataExtensionColumn
  case object `Post Code` extends DataExtensionColumn
  case object `Country` extends DataExtensionColumn
  case object `Account Name` extends DataExtensionColumn
  case object `Sort Code` extends DataExtensionColumn
  case object `Account number` extends DataExtensionColumn
  case object `Date of first payment` extends DataExtensionColumn
  case object `Date of second payment` extends DataExtensionColumn
  case object `Currency` extends DataExtensionColumn
  case object `Trial period` extends DataExtensionColumn
  case object `MandateID` extends DataExtensionColumn
  case object `Email` extends DataExtensionColumn

  case class ExactTargetException(message: String) extends Throwable(message)
}
