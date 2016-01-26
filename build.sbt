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
      "com.gu.i18n.CountryGroup",
      "com.gu.i18n.Country",
      "com.gu.memsub.promo.PromoCode",
      "com.gu.memsub.Subscription.ProductRatePlanId"
  ))

scalaVersion := "2.11.6"
scalacOptions ++= Seq("-feature")

val scalatestVersion = "2.2.4"

libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    PlayImport.specs2,
    "com.gu" %% "play-googleauth" % "0.3.3",
    "com.gu" %% "membership-common" % "0.145",
    "com.gu" %% "content-authorisation-common" % "0.1",
    "com.gu" %% "identity-test-users" % "0.5",
    "com.gu.identity" %% "identity-play-auth" % "0.14",
    "com.github.nscala-time" %% "nscala-time" % "2.0.0",
    "net.kencochrane.raven" % "raven-logback" % "6.0.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.scalactic" %% "scalactic" % scalatestVersion % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test",
    "com.gocardless" % "gocardless-pro" % "1.8.0",
    "com.squareup.okhttp" % "okhttp" % "2.4.0",
    "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT",
    "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "org.scalaz" %% "scalaz-core" % "7.1.3"
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
