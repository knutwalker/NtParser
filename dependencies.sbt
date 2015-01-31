scalaVersion in ThisBuild := "2.11.5"
crossScalaVersions in ThisBuild := List("2.10.4", "2.11.5")

libraryDependencies in ThisBuild ++= List(
  "org.apache.commons" % "commons-compress" % "1.9",
  "org.slf4j"          % "slf4j-api"        % "1.7.10"
)
