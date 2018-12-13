package services
import com.gu.i18n.{Country, Title}
import com.gu.memsub.Benefit._
import com.gu.memsub.Product.{Delivery, ZDigipack}
import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId, SubscriptionRatePlanChargeId}
import com.gu.memsub._
import com.gu.memsub.subsv2._
import com.gu.zuora.api.InvoiceTemplate
import model.{DeliveryRecipient, DigipackData, PaperData}
import org.joda.time.{Days, LocalDate}
import org.specs2.mutable.Specification
import touchpoint.ZuoraProperties

class CheckoutServiceTest extends Specification {

  implicit val today = new LocalDate("2016-07-22") // deterministic
  val address = DeliveryRecipient(Some(Title.Mx), Some("firstname"), Some("lastname"), Some("email@email.com"), Address("123 Fake St", "", "Town", "Kent", "123", "Blah"))
  val noPricing = PricingSummary(Map.empty)

  val paperPlan = new CatalogPlan[Delivery, PaperCharges, Current](
    id = ProductRatePlanId("p"),
    name = "name",
    description = "desc",
    charges = PaperCharges(Map(MondayPaper -> PricingSummary(Map.empty)), None),
    product = Delivery,
    saving = None,
    s = Status.current
  )

  val digiPlan = new CatalogPlan[ZDigipack, PaidCharge[Digipack.type, BillingPeriod], Current](
    id = ProductRatePlanId("p"),
    name = "name",
    description = "desc",
    charges = PaidCharge(Digipack, BillingPeriod.Month, PricingSummary(Map.empty), ProductRatePlanChargeId("foo"), SubscriptionRatePlanChargeId("")),
    product = Product.Digipack,
    saving = None,
    s = Status.current
  )

  val zuora = ZuoraProperties(defaultDigitalPackFreeTrialPeriod = Days.days(14), gracePeriodInDays = Days.days(2), invoiceTemplates = List(InvoiceTemplate("foo", Country.UK)))
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
  "Calculate the next available Friday correctly (if today is a Monday)" in {
    CheckoutService.determineFirstAvailableWeeklyDate(new LocalDate("2017-05-08")) mustEqual new LocalDate("2017-05-19")
  }


  "Calculate the next available Friday correctly (if today is a Wednesday)" in {
    CheckoutService.determineFirstAvailableWeeklyDate(new LocalDate("2017-05-17")) mustEqual new LocalDate("2017-05-26")
  }

    "Calculate the next available Friday correctly (if today is a Friday)" in {
      CheckoutService.determineFirstAvailableWeeklyDate(new LocalDate("2017-06-09")) mustEqual new LocalDate("2017-06-23")
    }

    "Calculate the next available Friday correctly (if today is a Sunday)" in {
      CheckoutService.determineFirstAvailableWeeklyDate(new LocalDate("2017-06-11")) mustEqual new LocalDate("2017-06-23")
    }

}
