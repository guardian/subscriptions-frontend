package controllers

import com.gu.i18n.CountryGroup
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}

object Binders {
  implicit object bindableCountryGroup extends QueryParsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )
}
