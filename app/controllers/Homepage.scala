package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.DigitalEdition
import model.DigitalEdition._
import utils.RequestCountry._
import play.api.mvc._

object Homepage extends Controller {

  def index = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.UK)
    val redirectUri = Uri.parse(request.uri) / getForCountryGroup(countryGroup).id
    Redirect(request.toUriWithCampaignParams(redirectUri), SEE_OTHER)
  }

  def indexUk = CachedAction(Ok(views.html.index()))

  def internationalHomepage(edition: DigitalEdition) = CachedAction(Ok(views.html.index_intl(edition)))

  def indexAu = internationalHomepage(AU)

  def indexUs = internationalHomepage(US)

  def indexInt = internationalHomepage(INT)
}
