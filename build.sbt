import sbtbuildinfo.Plugin.BuildInfoKey
import sbtbuildinfo.Plugin._

name := "frontend"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(magentaPackageName := name.value)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  ws
)

resolvers ++= Seq(
  "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
  Resolver.sonatypeRepo("releases"))


playArtifactDistSettings

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
  BuildInfoKey.constant("buildTime", System.currentTimeMillis)
)

buildInfoPackage := "frontend"

