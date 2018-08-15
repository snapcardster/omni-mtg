import Dependencies._
//import AssemblyKeys._

lazy val commonSettings = List(
  organization := "com.snapcardster",
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


// Select at least java 9 in intellij settings
// scalacOptions += "-target:jvm-1.9"
// javacOptions ++= Seq("-source", "1.9", "-target", "1.9")

// Need to add this directory as source root in intellij settings
// scalaSource in Compile := file("../common/src/main/scala")

// Material UI Components in Java FX
// https://github.com/jfoenixadmin/JFoenix
libraryDependencies += "com.jfoenix" % "jfoenix" % "9.0.4"

// https://mvnrepository.com/artifact/com.google.code.gson/gson
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"

// https://mvnrepository.com/artifact/commons-codec/commons-codec
libraryDependencies += "commons-codec" % "commons-codec" % "1.11"

// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.2.11"


libraryDependencies += scalaTest % Test
