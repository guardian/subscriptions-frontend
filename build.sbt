import play.sbt.PlayImport
import scala.sys.process._

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

scalaVersion := "2.12.14"
scalacOptions ++= Seq("-feature")

val scalatestVersion = "3.2.9"
val jacksonVersion = "2.10.0"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) // for simulacrum

libraryDependencies ++= Seq(
  "com.gu" %% "ophan-event-model" % "0.0.17" excludeAll ExclusionRule(organization = "com.typesafe.play"),
  "com.gu" %% "fezziwig" % "1.6",
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "io.circe" %% "circe-core" % "0.12.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.gu" %% "acquisitions-value-calculator-client" % "2.0.5",
  "com.squareup.okhttp3" % "okhttp" % "3.9.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.typelevel" %% "simulacrum" % "1.0.1",
  "org.typelevel" %% "cats-core" % "2.6.1",
  "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.465",
  "com.gu" %% "thrift-serializer" % "4.0.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5.1"
)
libraryDependencies ++= Seq(
    ws,
    filters,
    jodaForms,
    PlayImport.specs2 % "test",
    "com.gu" %% "membership-common" % "0.589",
    "com.gu.identity" %% "identity-auth-play" % "3.248",
    "com.gu" %% "identity-test-users" % "0.6",
    "com.gu.play-googleauth" %% "play-v28" % "2.1.1",
    "com.gu" %% "identity-test-users" % "0.6",
    "com.gu" %% "content-authorisation-common" % "0.6",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "io.sentry" % "sentry-logback" % "1.7.5",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "com.gocardless" % "gocardless-pro" % "2.7.0",
    "org.scalaz" %% "scalaz-core" % "7.2.7",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.564",
    "com.gu" % "kinesis-logback-appender" % "1.4.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
    "com.google.guava" % "guava" % "25.0-jre", //-- added explicitly - snyk report avoid logback vulnerability
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion, //added explicitly to avoid snyk vulnerability
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion, //added explicitly to avoid snyk vulnerability
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion, //added explicitly to avoid snyk vulnerability
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion, //added explicitly to avoid snyk vulnerability
    "ch.qos.logback" % "logback-classic" % "1.2.3", //-- added explicitly - snyk report avoid logback vulnerability
    "org.bouncycastle" % "bcprov-jdk15on" % "1.69" // https://snyk.io/vuln/SNYK-JAVA-ORGBOUNCYCASTLE-1035561
)

testOptions in Test ++= Seq(
    Tests.Argument("-oD") // display full stack errors and execution times in Scalatest output
)
traceLevel in Test := 0

testResultLogger in Test := new ScalaTestWithExitCode

javaOptions in Test += "-Dconfig.file=test/conf/test.conf"

resolvers ++= Seq(
    "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
    "Guardian Github Snapshots" at "https://guardian.github.io/maven/repo-snapshots",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
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

riffRaffArtifactResources += (file("cloud-formation/subscriptions-app.cf.yaml"), "cfn/cfn.yaml")

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
addCommandAlias("play-artifact", "riffRaffNotifyTeamcity")
