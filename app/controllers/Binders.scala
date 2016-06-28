package controllers

import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.ProductFamily
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}
import play.api.mvc.PathBindable.{Parsing => PathParsing}

import scala.reflect.runtime.universe._

object Binders {
  def applyNonEmpty[A: TypeTag](f: String => A)(s: String): A =
    if (s.isEmpty) {
      val msg = s"Cannot build a ${implicitly[TypeTag[A]].tpe} from an empty string"
      throw new IllegalArgumentException(msg)
    } else f(s)

  implicit object bindableCountryGroup extends QueryParsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )

  implicit object bindablePrpId extends QueryParsing[ProductRatePlanId](
    applyNonEmpty(ProductRatePlanId), _.get, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )

  implicit object bindablePromoCode extends QueryParsing[PromoCode](
    applyNonEmpty(PromoCode), _.get, (key: String, _: Exception) => s"Cannot parse parameter $key as a PromoCode"
  )

  implicit object bindableProductFamilyPath extends PathParsing[ProductFamily](
    applyNonEmpty(id => ProductFamily.fromId(id).get), _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a Product"
  )

  implicit object bindableProductFamilyQuery extends QueryParsing[ProductFamily](
    applyNonEmpty(id => ProductFamily.fromId(id).get), _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a Product"
  )

  implicit object bindableCountry extends QueryParsing[Country](
    alpha2 => CountryGroup.countryByCode(alpha2).get, _.alpha2, (key: String, _: Exception) => s"Cannot parse parameter $key as a Country"
  )
}
