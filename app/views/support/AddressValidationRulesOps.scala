package views.support

import model._
import play.twirl.api.Html

object AddressValidationRulesOps {
  implicit class ValidationRulesToAttributes(rules: AddressValidationRules) {
    private val toAttributesMap: Map[String, String] = {
      val subdivisionRequired = "data-subdivision-required"
      val subdivisionList = "data-subdivision-list"

      rules.subdivision match {
        case SubdivisionOptional => Map(subdivisionRequired -> "false")
        case SubdivisionRequired => Map(subdivisionRequired -> "true")
        case SubdivisionList(s) => Map(subdivisionRequired -> "true", subdivisionList -> s.mkString(","))
      }
    }

    def toAttributes: Html = Html(toAttributesMap.map { case (k, v) => s"""$k="$v"""" }.mkString(" "))
  }
}
