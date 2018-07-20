import Dependencies._
//import AssemblyKeys._

lazy val commonSettings = List(
  organization := "com.example",
  scalaVersion := "2.12.5",
  version := "0.1.0-SNAPSHOT"
)

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


// scalacOptions += "-target:jvm-1.9"
// javacOptions ++= Seq("-source", "1.9", "-target", "1.9")

// Material UI Components in Java FX
// https://github.com/jfoenixadmin/JFoenix
libraryDependencies += "com.jfoenix" % "jfoenix" % "9.0.4"

// Needed for REST Call
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.2.11"

// libraryDependencies += "org.scala-lang.modules" % "scala-xml" % "1.1.0"
// fork := true

// Needed for JSON parsing
// https://mvnrepository.com/artifact/javax.json/javax.json-api
libraryDependencies += "javax.json" % "javax.json-api" % "1.1.2"

// https://mvnrepository.com/artifact/org.glassfish/javax.json
libraryDependencies += "org.glassfish" % "javax.json" % "1.1.2"


libraryDependencies += scalaTest % Test
