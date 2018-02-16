package controllers

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.memsub.{ProductFamily, SupplierCode, SupplierCodeBuilder}
import play.api.mvc.PathBindable.{Parsing => PathParsing}
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}

import scala.reflect.runtime.universe._

object Binders {
  def applyNonEmpty[A: TypeTag](f: String => A)(s: String): A =
    if (s.isEmpty) {
      val msg = s"A paremeter value must be provided."
      throw new IllegalArgumentException(msg)
    } else f(s)

  implicit object bindableCountryGroup extends QueryParsing[CountryGroup](
    CountryGroup.byId(_).get, _.id, (key: String, _: Exception) => s"URL parameter $key is not a valid CountryGroup"
  )

  implicit object bindablePrpId extends QueryParsing[ProductRatePlanId](
    applyNonEmpty(ProductRatePlanId), _.get, (key: String, e: Exception) => s"URL parameter $key is not a valid ProductRatePlanId. ${e.getMessage}"
  )

  implicit object bindablePromoCode extends QueryParsing[PromoCode](
    applyNonEmpty(NormalisedPromoCode.safeFromString), _.get, (key: String, e: Exception) => s"URL parameter $key is not a valid PromoCode. ${e.getMessage}"
  )

  implicit object bindableSupplierCode extends QueryParsing[SupplierCode](
    SupplierCodeBuilder.buildSupplierCode(_).get, _.get, (key: String, _: Exception) => s"URL parameter $key is not a valid SupplierCode"
  )

  implicit object bindableProductFamilyPath extends PathParsing[ProductFamily](
    ProductFamily.fromId(_).get, _.id, (key: String, _: Exception) => s"URL parameter $key is not a valid Product"
  )

  implicit object bindableProductFamilyQuery extends QueryParsing[ProductFamily](
    ProductFamily.fromId(_).get, _.id, (key: String, _: Exception) => s"URL parameter $key is not a valid Product"
  )

  implicit object bindableCountry extends QueryParsing[Country](
    CountryGroup.countryByCode(_).get, _.alpha2, (key: String, _: Exception) => s"URL parameter $key is not a valid Country"
  )

  implicit object bindableCurrency extends QueryParsing[Currency](
    Currency.fromString(_).get, _.iso, (key: String, _: Exception) => s"URL parameter $key is not a valid Currency"
  )
}
