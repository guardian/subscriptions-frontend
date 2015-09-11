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

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  PlayImport.specs2,
  "com.gu" %% "membership-common" % "0.88-SNAPSHOT",
  "com.gu" %% "play-googleauth" % "0.3.1",
  "com.gu" %% "identity-test-users" % "0.5",
  "com.gu.identity" %% "identity-play-auth" % "0.8",
  "com.github.nscala-time" %% "nscala-time" % "2.0.0",
  "net.kencochrane.raven" % "raven-logback" % "6.0.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.44.0" % "test",
  "com.gocardless" % "gocardless-pro" % "1.0.0",
  "com.squareup.okhttp" % "okhttp" % "2.4.0"
)

testOptions in Test ++= Seq(
  Tests.Argument("-oFD") // display full stack errors and execution times in Scalatest output
)

testResultLogger in Test := new TestResultLogger {
  import sbt.Tests._

  def run(log: Logger, results: Output, taskName: String): Unit = {
    results.overall match {
      case TestResult.Error | TestResult.Failed => sys.exit(1)
      case _ =>
    }
  }
}

javaOptions in Test += "-Dconfig.file=test/conf/application.conf"

resolvers ++= Seq(
  "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  Resolver.sonatypeRepo("releases"))

addCommandAlias("devrun", "run -Dconfig.resource=DEV.conf 9200")
addCommandAlias("fast-test", "testOnly -- -l Acceptance")
addCommandAlias("acceptance-test", "testOnly acceptance.Main")

playArtifactDistSettings
