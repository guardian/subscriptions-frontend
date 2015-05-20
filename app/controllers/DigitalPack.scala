package controllers

import actions.CommonActions
import play.api.mvc._

case class DigitalEdition(id: String, name: String, price: String, cmp: String)

object DigitalEdition {

  object UK extends DigitalEdition("uk", "UK", "£11.99", "dis_2408")
  object US extends DigitalEdition("us", "US", "$19.99", "dis_2378")
  object AU extends DigitalEdition("au", "Australia", "$21.50", "dis_2379")

}

object DigitalPack extends Controller with CommonActions {

  def uk = cachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.UK))
  }

  def us = cachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.US))
  }

  def au = cachedAction {
    Ok(views.html.digitalpack.info(DigitalEdition.AU))
  }

  def selectCountry = cachedAction {
    Ok(views.html.digitalpack.country())
  }

  def handleCountry = cachedAction(parse.tolerantFormUrlEncoded) { request =>
    val country: Option[String] = request.body.get("country").map(_.head)
    if(country == Some("uk")) {
      Redirect("/checkout")
    } else {
      Redirect("https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=dga38&CMP=FAB_3062")
    }
  }

}