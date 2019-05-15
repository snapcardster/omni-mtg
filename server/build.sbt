name := "server"

version := "1.0"

lazy val `server` = (project in file(".")).enablePlugins(PlayScala)



lazy val root = (project in file(".")).
  settings(
    //  assemblySettings : _*,
    inThisBuild(commonSettings),
    mainClass in assembly := Some("com.snapcardster.omnimtg.Main"),
    assemblyJarName in assembly := "omni-mtg.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    name := "OmniMtg"
  )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  jdbc, ehcache, ws, specs2 % Test, guice,
  "io.swagger" %% "swagger-scala-module" % "1.0.3",
  "com.jfoenix" % "jfoenix" % "9.0.4",
  // https://mvnrepository.com/artifact/com.google.code.gson/gson
  "com.google.code.gson" % "gson" % "2.8.5",
  // https://mvnrepository.com/artifact/commons-codec/commons-codec
  "commons-codec" % "commons-codec" % "1.11",
  // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
  "commons-lang" % "commons-lang" % "2.6",
  "javax.xml.bind" % "jaxb-api" % "2.2.11"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

