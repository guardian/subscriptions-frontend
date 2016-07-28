package services
import com.gu.memsub._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.subscriptions.{DigitalProducts, PhysicalProducts, ProductPlan}
import model.{DigipackData, PaperData}
import org.joda.time.Days
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import touchpoint.ZuoraProperties
import scalaz.syntax.nel._

class CheckoutServiceTest extends Specification {

  val noPricing = PricingSummary(Map.empty)
  implicit val today = new LocalDate("2016-07-22") // deterministic
  val address = Address("123 Fake St", "", "Town", "Kent", "123", "Blah")
  val noPricing = PricingSummary(Map.empty)

  val paperPlan = ProductPlan(ProductRatePlanId(""), "", "", PhysicalProducts((MondayPaper -> noPricing).wrapNel, List.empty), "", None, BillingPeriod.month)
  val digiPlan = ProductPlan(ProductRatePlanId(""), "", "", DigitalProducts((Digipack -> noPricing).wrapNel), "", None, BillingPeriod.month)

  val zuora = ZuoraProperties(paymentDelayInDays = Days.days(14), gracePeriodInDays = Days.days(2))
  val paperData = Left(PaperData(new LocalDate("2016-07-30"), address, None, paperPlan))
  val digipackData = Right(DigipackData(digiPlan))

  "Free trial calculator" should {

    "Return the zuora payment delay + the grace period for a digipack sub" in {
      CheckoutService.paymentDelay(digipackData, zuora) mustEqual Days.days(16)
    }

    "Return the days between today and the start date for paper" in {
      CheckoutService.paymentDelay(paperData, zuora) mustEqual Days.days(8)
    }

  }
}
