package acceptance

import acceptance.util.AcceptanceTest
import org.scalatest.{FreeSpec, Matchers}
import services.GetAddressIOService
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class GetAddressIOServiceTest extends FreeSpec with Matchers {

  val getAddressIOService: GetAddressIOService = new GetAddressIOService()

	"getAddressIOService should successfully retrieve a correct postcode'" taggedAs AcceptanceTest in {
    noException should be thrownBy Await.result(getAddressIOService.find("N1 9AG"), 2.seconds)
	}

}
