import play.sbt.PlayImport

name := "subscriptions-frontend"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(
    PlayScala,
    BuildInfoPlugin
).settings(
    magentaPackageName := "frontend",
    buildInfoKeys := Seq[BuildInfoKey](
	name,
	BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse (try {
	    "git rev-parse HEAD".!!.trim
	} catch {
	    case e: Exception => "unknown"
	})),
	BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
	BuildInfoKey.constant("buildTime", System.currentTimeMillis)
    ),
    buildInfoPackage := "app",
    buildInfoOptions += BuildInfoOption.ToMap
    )
  .settings(play.sbt.routes.RoutesKeys.routesImport ++= Seq(
      "controllers.Binders._",
      "model.DigitalEdition",
      "com.gu.i18n.CountryGroup",
      "com.gu.i18n.Country",
      "com.gu.i18n.Currency",
      "com.gu.memsub.promo.PromoCode",
      "com.gu.memsub.SupplierCode",
      "com.gu.memsub.Subscription.ProductRatePlanId"
  ))

scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature")

val scalatestVersion = "3.0.0"

libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    PlayImport.specs2,
    "com.gu" %% "membership-common" % "0.360",
    "com.gu" %% "memsub-common-play-auth" % "0.8",
    "com.gu" %% "identity-test-users" % "0.6",
    "com.gu" %% "content-authorisation-common" % "0.1",
    "com.github.nscala-time" %% "nscala-time" % "2.8.0",
    "net.kencochrane.raven" % "raven-logback" % "6.0.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.scalactic" %% "scalactic" % scalatestVersion % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "3.0.1" % "test",
    "org.seleniumhq.selenium" % "htmlunit-driver" % "2.23" % "test",
    "io.github.bonigarcia" % "webdrivermanager" % "1.4.10" % "test",
    "com.gocardless" % "gocardless-pro" % "1.16.0",
    "com.squareup.okhttp3" % "okhttp" % "3.4.1" % "test",
    "org.scalaz" %% "scalaz-core" % "7.1.3",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.amazonaws" % "aws-java-sdk-sqs" % "1.10.50",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.3"
)

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)
traceLevel in Test := 0

testResultLogger in Test := new ScalaTestWithExitCode

javaOptions in Test += "-Dconfig.file=test/acceptance/conf/acceptance-test.conf"

resolvers ++= Seq(
    "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
    "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-snapshots",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("releases"))

addCommandAlias("devrun", "run -Dconfig.resource=DEV.conf 9200")
addCommandAlias("fast-test", "testOnly -- -l Acceptance")
addCommandAlias("acceptance-test", "testOnly acceptance.CheckoutSpec")

playArtifactDistSettings
