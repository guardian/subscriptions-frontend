import play.sbt.PlayImport

name := "frontend"

version := "1.0-SNAPSHOT"

def commitId(): String = try {
    "git rev-parse HEAD".!!.trim
} catch {
    case _: Exception => "unknown"
}

lazy val root = (project in file(".")).enablePlugins(
    PlayScala,
    BuildInfoPlugin,
    RiffRaffArtifact,
    JDebPackaging
).settings(
    buildInfoKeys := Seq[BuildInfoKey](
	name,
	BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse commitId()),
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

scalaVersion := "2.11.12"
scalacOptions ++= Seq("-feature")

val scalatestVersion = "3.0.4"

libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    jodaForms,
    PlayImport.specs2 % "test",
    "com.gu" %% "membership-common" % "0.497",
    "com.gu.identity" %% "identity-play-auth" % "2.1",
    "com.gu" %% "identity-test-users" % "0.6",
    "com.gu" %% "play-googleauth" % "0.7.2",
    "com.gu" %% "identity-test-users" % "0.6",
    "com.gu" %% "content-authorisation-common" % "0.1",
    "com.gu" %% "tip" % "0.1.1",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "com.getsentry.raven" % "raven-logback" % "8.0.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.scalactic" %% "scalactic" % scalatestVersion % "test",
    "org.seleniumhq.selenium" % "selenium-java" % "3.7.1" % "test",
    "org.seleniumhq.selenium" % "htmlunit-driver" % "2.28.1" % "test",
    "io.github.bonigarcia" % "webdrivermanager" % "1.7.2" % "test",
    "com.gocardless" % "gocardless-pro" % "2.7.0",
    "com.squareup.okhttp3" % "okhttp" % "3.4.2" % "test",
    "org.scalaz" %% "scalaz-core" % "7.2.7",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.231",
    "net.databinder.dispatch" %% "dispatch-core" % "0.13.2",
    "com.gu" % "kinesis-logback-appender" % "1.4.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.2",
    "com.gu" %% "acquisition-event-producer-play26" % "2.0.4",
    "com.google.guava" % "guava" % "23.4-jre" % "test"
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
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayRepo("guardian", "ophan")
)

import com.typesafe.sbt.packager.archetypes.systemloader.ServerLoader.Systemd
enablePlugins(SystemdPlugin)
serverLoading in Debian := Some(Systemd)
debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "Subscriptions Dev <subscriptions.dev@theguardian.com>"
packageSummary := "Subscription Frontend"
packageDescription := """Subscription Frontend"""

riffRaffPackageType := (packageBin in Debian).value

routesGenerator := InjectedRoutesGenerator

javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null",
      "-J-XX:MaxRAMFraction=2",
      "-J-XX:InitialRAMFraction=2",
      "-J-XX:MaxMetaspaceSize=500m",
      "-J-XX:+PrintGCDetails",
      "-J-XX:+PrintGCDateStamps",
      s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
    )

addCommandAlias("devrun", "run  9200")
addCommandAlias("prodrun", "run 9200")
addCommandAlias("fast-test", "testOnly -- -l Acceptance")
addCommandAlias("acceptance-test", "testOnly acceptance.CheckoutSpec")
addCommandAlias("play-artifact", "riffRaffNotifyTeamcity")


