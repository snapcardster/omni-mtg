// import AssemblyKeys._

lazy val commonSettings = Seq(
  name := "OmniMtg",
  version := "1.0",
  version := "0.1-SNAPSHOT",
  organization := "omnimtg",
  scalaVersion := "2.12.2",
  maintainer := "info@snapcardster.com"
)

def println(x: Any): Unit = {
  System.out.println(String.valueOf(x))
}

lazy val `server` = (project in file(".")).settings(commonSettings: _*).enablePlugins(PlayScala)

// https://github.com/sbt/sbt-assembly/blob/master/Migration.md
// "app" is important for play projects
lazy val root = (project in file("app"))
  .settings(commonSettings: _*)
  .settings(
    mainClass in assembly := Some("app.Main"),
    assemblyJarName in assembly := "omni-mtg.jar",
    /*assemblyMergeStrategy in assembly := {
      // Building fat jar without META-INF
      case PathList("META-INF") =>
        println("META-INF")
        System.exit(1)
        MergeStrategy.discard
      /*case PathList("META-INF", xs@_*) =>
        println("META-INF" -> xs)
        MergeStrategy.discard*/
      // Take last config file
      case "reference-overrides.conf" =>
        System.exit(2)
        println("PL(reference-overrides.conf)")
        MergeStrategy.discard
      case PathList("reference-overrides.conf", _) =>
        System.exit(3)
        println("PL(reference-overrides.conf)")
        MergeStrategy.discard
      case PathList("reference-overrides.conf") =>
        System.exit(4)
        println("PL(reference-overrides.conf)")
        MergeStrategy.discard
      /*case PathList(ps@_*) if ps.last.endsWith(".conf") =>
        println("endsWith .conf" -> ps)
        MergeStrategy.last*/
      case o =>
        System.exit(5)
        println("old" -> o)
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(o)
    },*/
    // name := "OmniMtg"
  )

// sourceDirectory += baseDirectory(_ / "app")
// sourceDirectories += baseDirectory(_ / "app")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  /*jdbc, ehcache,*/
  /*ws,*/
  /*specs2 % Test,*/
  "junit" % "junit" % "4.4" % Test,
  guice,
  // "io.swagger" %% "swagger-scala-module" % "1.0.3",
  // "com.jfoenix" % "jfoenix" % "9.0.4",
  // https://mvnrepository.com/artifact/com.google.code.gson/gson
  "com.google.code.gson" % "gson" % "2.8.5",
  // https://mvnrepository.com/artifact/commons-codec/commons-codec
  "commons-codec" % "commons-codec" % "1.11" // ,
  // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
  // "commons-lang" % "commons-lang" % "2.6" //,
  // already contained in typesafe.play jar: "javax.xml.bind" % "jaxb-api" % "2.2.11"
).map(_.exclude("org.apache.logging.log4j", "org.apache.logging.log4j"))
  .map(_.exclude("commons-logging", "commons-logging"))
  .map(_.exclude("log4j-cloudwatch-appender", "log4j-cloudwatch-appender"))
  .map(_.exclude("com.typesafe.play", "com.typesafe.play"))
  .map(_.exclude("com.typesafe.play", "play_2.12"))
  .map(_.exclude("com.typesafe.play", "play"))
  .map(_.exclude("com.typesafe.play", "play-akka-http-server_2.12-2.7.2"))
  .map(_.exclude("com.typesafe.play", "play-akka-http-server"))

/*unmanagedResourceDirectories in Test ++= {
  Seq(baseDirectory(_ / "target/web/public/test"))
}*/
/*
libraryDependencies ~= {
  _ map {
    case m if m.organization == "com.typesafe.play" =>
      m.exclude("commons-logging", "commons-logging")
        .exclude("com.typesafe.play", "sbt-link")
    case m => m
  }
}
*/