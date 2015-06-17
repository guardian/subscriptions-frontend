package testUtils

import play.api.{Application, GlobalSettings}
import play.api.test.FakeApplication

object PlayUtils {
  val fakeApplicationWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
    override def onStart(app: Application) {}
  }))

}
