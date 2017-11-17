
resolvers += Resolver.typesafeRepo("releases") // Play seems to require quite a few things from here

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.18")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))
