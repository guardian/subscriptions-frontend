package views.support

import model._
import play.twirl.api.Html

object AddressValidationRulesOps {
  implicit class ValidationRulesToAttributes(rules: AddressValidationRules) {
    private val toAttributesMap: Map[String, String] = {
      val postcodeRequiredAttr = "data-postcode-required"
      val subdivisionRequired = "data-subdivision-required"
      val subdivisionList = "data-subdivision-list"

      val postcode = rules.postcode match {
        case PostcodeRequired => Map(postcodeRequiredAttr -> "true")
        case _ => Map(postcodeRequiredAttr -> "false")
      }

      val subdivision = rules.subdivision match {
        case SubdivisionOptional => Map(subdivisionRequired -> "false")
        case SubdivisionRequired => Map(subdivisionRequired -> "true")
        case SubdivisionList(s) => Map(subdivisionRequired -> "true", subdivisionList -> s.mkString(","))
      }

      postcode ++ subdivision
    }

    def toAttributes: Html = Html(toAttributesMap.map { case (k, v) => s"""$k="$v"""" }.mkString(" "))
  }
}
