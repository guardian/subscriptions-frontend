import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._
import testUtils.PlayUtils.fakeApplicationWithGlobal

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {


  "Application" should {
    
    "render the index page" in new WithApplication(app = fakeApplicationWithGlobal) {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Subscriptions and Membership")
    }
  }
}
