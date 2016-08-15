package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import com.gu.memsub.SupplierCode
import com.gu.memsub.SupplierCodeBuilder
import model.DigitalEdition._
import utils.RequestCountry._
import play.api.mvc._
import SessionKeys.SupplierTrackingCode

object Homepage extends Controller {

  def index = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.UK)
    val digitalEdition = getById(countryGroup.id).getOrElse(INT)
    Redirect(routes.Homepage.landingPage(digitalEdition.id).url, request.queryString, SEE_OTHER)
  }

  def landingPage(code: String) = CachedAction {
    getById(code).fold {
      NotFound(views.html.error404())
    } {
      case UK => Ok(views.html.index())
      case digitalEdition => Ok(views.html.index_intl(digitalEdition))
    }
  }

  def supplierRedirect(supplierCodeStr: String) = NoCacheAction { implicit request =>
    val url = routes.Homepage.landingPage(UK.id).url
    SupplierCodeBuilder.buildSupplierCode(supplierCodeStr).fold {
      val newSession = request.session - SupplierTrackingCode
      Redirect(url, request.queryString, SEE_OTHER).withSession(newSession)  // clear any supplier code
    } { supplierCode =>
      val newQueryString = request.queryString + ("INTCMP" -> Seq(s"FROM_S_${supplierCode.get}"))
      Redirect(url, newQueryString, SEE_OTHER).withSession(request.session + (SupplierTrackingCode -> supplierCode.get))
    }
  }
}
