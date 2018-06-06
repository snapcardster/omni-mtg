import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.13.0",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "OmniMtg"
  )

libraryDependencies += "com.jfoenix" % "jfoenix" % "9.0.4"

libraryDependencies += scalaTest % Test
