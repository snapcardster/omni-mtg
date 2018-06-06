import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.5",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "OmniMtg"
  )

// Material UI Components in Java FX
libraryDependencies += "com.jfoenix" % "jfoenix" % "9.0.4"

// Needed for REST Call
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.2.11"

libraryDependencies += scalaTest % Test
