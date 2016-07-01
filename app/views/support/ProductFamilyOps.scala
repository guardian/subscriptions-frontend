package views.support

import com.gu.memsub.{Digipack, Paper, ProductFamily}

object ProductFamilyOps {

  implicit class ProductFamilyName(in: ProductFamily) {
    def title: String = in match {
      case Digipack => "The Guardian Digital Pack"
      case Paper => "Guardian & Observer papers"
    }

    def subtitle: String = in match {
      case Digipack => "(Daily Edition + Guardian App Premium Tier)"
      case Paper => "Text here"
    }

    def packImage: String = in match {
      case Digipack => "images/digital-pack.png"
      case Paper => "images/digital-pack.png"
    }

    def changeRatePlanText: String = in match {
      case Digipack => "Change payment frequency"
      case Paper => "Change package"
    }

  }

}
