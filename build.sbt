name := """ntparser"""

organization := "de.knutwalker"

description := "A fast parser for .nt files"

scalaVersion := "2.11.2"

crossScalaVersions := List("2.10.4", "2.11.2")

scalacOptions := List(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-target:jvm-1.7",
  "-encoding", "UTF-8")
