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
    BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
      "git rev-parse HEAD".!!.trim
    } catch { case e: Exception => "unknown" })),
    BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
    BuildInfoKey.constant("buildTime", System.currentTimeMillis)
  ),
  buildInfoPackage := "app",
  buildInfoOptions += BuildInfoOption.ToMap
)

scalaVersion := "2.11.6"

val cxfV = "2.7.5"
val cxfUtilsV = "2.7.0"
val exactTargetDeps = Seq(
  "com.exacttarget" % "fuelsdk" % "1.0.3" from "https://github.com/salesforcefuel/FuelSDK-Java/releases/download/v1.0.3/fuel-java-1.0.3.jar",
  "commons-beanutils" % "commons-beanutils" % "1.9.2",
  "log4j" % "log4j" % "1.2.17",
  "com.google.code.gson" % "gson" % "2.3.1",
  "org.apache.cxf" % "cxf-bundle-minimal" % cxfV,
  "org.apache.cxf" % "cxf-tools-common" % cxfV,
  "org.apache.cxf" % "cxf-tools-common" % cxfV,
  "org.apache.cxf.xjc-utils" % "xjc-utils" % cxfUtilsV,
  "org.apache.cxf.xjc-utils" % "cxf-xjc-runtime" % cxfUtilsV
)

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  PlayImport.specs2,
  "com.gu" %% "membership-common" % "0.79-SNAPSHOT",
  "com.gu" %% "play-googleauth" % "0.3.0",
  "com.gu.identity" %% "identity-play-auth" % "0.5",
  "com.github.nscala-time" %% "nscala-time" % "2.0.0",
  "net.kencochrane.raven" % "raven-logback" % "6.0.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.44.0" % "test",
  "com.gocardless" % "gocardless-pro" % "1.0.0"
) ++ exactTargetDeps

testOptions in Test ++= Seq(
  Tests.Argument("-oFD") // display full stack errors and execution times in Scalatest output
)

javaOptions in Test += "-Dconfig.file=test/conf/application.conf"

resolvers ++= Seq(
  "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  Resolver.sonatypeRepo("releases"))

addCommandAlias("devrun", "run -Dconfig.resource=DEV.conf 9200")
addCommandAlias("fast-test", "testOnly -- -l Acceptance")
addCommandAlias("acceptance-test", "testOnly acceptance.Main")

playArtifactDistSettings
