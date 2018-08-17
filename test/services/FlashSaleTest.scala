package services

import org.joda.time.DateTime
import org.specs2.mutable.Specification

object TestPromoCodes {
  val flashSaleIntcmp: Map[String, String] =
    Map(
      "promocode1" -> "super_special_value",
      "promocode2" -> "super_special_value2"
    )
}

object FlashSaleEffectiveNow extends FlashSale {
  val now = new DateTime()

  override val startTime = now.minusWeeks(1)
  override val endTime = now.plusWeeks(1)

  override val flashSaleIntcmp = TestPromoCodes.flashSaleIntcmp
}

object FlashSaleExpired extends FlashSale {
  val now = new DateTime()

  override val startTime = now.minusWeeks(2)
  override val endTime = now.minusWeeks(1)

  override val flashSaleIntcmp = TestPromoCodes.flashSaleIntcmp

}

class FlashSaleTest extends Specification {

  "intcmp" should {

    "return the default promo code-based value when not in a flash sale period" in {
      val expected = "FROM_P_promocode1"
      FlashSaleExpired.intcmp("promocode1") shouldEqual (expected)
    }

    "return the custom value when in a flash sale period" in {
      val expected = "super_special_value"
      FlashSaleEffectiveNow.intcmp("promocode1") shouldEqual (expected)
    }

    "return the default in a flash sale period if the promo code doesn't have a custom intcmp value" in {
      val expected = "FROM_P_ABCD"
      FlashSaleEffectiveNow.intcmp("ABCD") shouldEqual (expected)
    }
  }
}


