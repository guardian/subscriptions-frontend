package views.support

import scala.math.BigDecimal.RoundingMode

/**
  * Created by tverran on 12/08/2016.
  */
object FloatOps {

  implicit class OpsFloat(f: Float) {
    def currency: String = BigDecimal.decimal(f).setScale(2, RoundingMode.CEILING).toString()
  }

}
