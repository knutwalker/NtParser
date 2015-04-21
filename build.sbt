import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStep
import ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages
import xerial.sbt.Sonatype.SonatypeKeys.{profileName, sonatypeReleaseAll}

lazy val core = project enablePlugins AutomateHeaderPlugin settings (
  ntparserSettings,
  publishThis,
  name := "ntparser",
  libraryDependencies ++= List(
    "org.apache.commons" % "commons-compress" % "1.9",
    "org.slf4j"          % "slf4j-api"        % "1.7.10"))


lazy val model = project in file("models") / "nt" enablePlugins AutomateHeaderPlugin dependsOn core settings (
  ntparserSettings,
  publishThis,
  name := "ntparser-model")


lazy val jena = project in file("models") / "jena" enablePlugins AutomateHeaderPlugin dependsOn core settings (
  ntparserSettings,
  publishThis,
  name := "ntparser-jena",
  libraryDependencies += "org.apache.jena" % "jena-core" % "2.12.1")


lazy val examples = project enablePlugins AutomateHeaderPlugin dependsOn (core, model, jena) settings ntparserSettings


lazy val tests = project enablePlugins AutomateHeaderPlugin dependsOn (core, model, jena) settings (
  ntparserSettings,
  libraryDependencies ++= List(
    "org.scalatest"     %% "scalatest"     % "2.2.4"  % "test",
    "org.scalacheck"    %% "scalacheck"    % "1.12.1" % "test",
    "org.apache.commons" % "commons-lang3" % "3.3.2"  % "test"))


lazy val parent = project in file(".") aggregate (core, model, jena, examples, tests) dependsOn (core, model, jena, examples, tests) settings ntparserSettings


// ====================================================================

lazy val ntparserSettings =
  projectSettings ++ compilerSettings ++ buildSettings ++ otherSettings

lazy val projectSettings  = List(
          organization := "de.knutwalker",
  organizationHomepage := Some(url(s"https://github.com/${githubUser.value}/")),
              homepage := Some(url(s"https://github.com/${githubUser.value}/${githubRepo.value}")),
             startYear := Some(2014),
            maintainer := "Paul Horn",
            githubUser := "knutwalker",
            githubRepo := "NtParser",
           description := "A fast parser for .nt files",
          scalaVersion := "2.10.5",
    crossScalaVersions := scalaVersion.value :: "2.11.6" :: Nil)

lazy val compilerSettings = List(
  scalacOptions ++= {
    val crossOpts = scalaBinaryVersion.value match {
      case "2.11" => List(
        "-Xlint:_",
        "-Yconst-opt",
        "-Ywarn-infer-any",
        "-Ywarn-unused",
        "-Ywarn-unused-import")
      case _      => List(
        "-Xlint")
    }
    crossOpts ++ List(
      "-deprecation",
      "-encoding",  "UTF-8",
      "-feature",
      "-language:_",
      "-optimise",
      "-unchecked",
      "-target:jvm-1.7",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Yclosure-elim",
      "-Ydead-code",
      "-Yinline",
      "-Yno-adapted-args",
      "-Yinline-handlers",
      "-Yinline-warnings",
      "-Ywarn-adapted-args",
      "-Ywarn-inaccessible",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen")},
  scalacOptions in (Compile, console) ~= (_ filterNot (x => x == "-Xfatal-warnings" || x.startsWith("-Ywarn"))),
  scalacOptions in Test += "-Yrangepos")

lazy val buildSettings = List(
  initialCommands in console := """import de.knutwalker.ntparser._""",
             cleanKeepFiles ++= List("resolution-cache", "streams").map(target.value / _),
               updateOptions ~= (_.withCachedResolution(true)),
                 logBuffered := false,
                 shellPrompt := { state =>
  import scala.Console._
  val name = Project.extract(state).currentRef.project
  val color = name match {
    case "core"           => GREEN
    case "model" | "jena" => CYAN
    case "examples"       => MAGENTA
    case "tests"          => BLUE
    case _                => WHITE
  }
  (if (name == "parent") "" else s"[$color$name$RESET] ") + "> "
})

lazy val otherSettings = List(
  headers := {
    val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = List(startYear.value.getOrElse(thisYear), thisYear).distinct.mkString(" – ")
    Map(
      "java"  -> Apache2_0(years, maintainer.value),
      "scala" -> Apache2_0(years, maintainer.value))
  },
  coverageExcludedPackages := "de.knutwalker.ntparser.examples.*")


lazy val publishThis = releaseSettings ++ sonatypeSettings ++ List(
  publishArtifact in Test := false,
                 licenses += "Apache License, Verison 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
               tagComment := s"Release version ${version.value}",
            commitMessage := s"Set version to ${version.value}",
              versionBump := sbtrelease.Version.Bump.Minor,
  pomExtra := {
    <scm>
      <connection>scm:git:https://github.com/{githubUser.value}/{githubRepo.value}.git</connection>
      <developerConnection>scm:git:ssh://git@github.com:{githubUser.value}/{githubRepo.value}.git</developerConnection>
      <tag>master</tag>
      <url>https://github.com/{githubUser.value}/{githubRepo.value}</url>
    </scm>
    <developers>
      <developer>
        <id>{githubUser.value}</id>
        <name>{maintainer.value}</name>
        <url>{organizationHomepage.value.get}</url>
      </developer>
    </developers>
  },
  pomPostProcess := { (node) =>
    val rewriteRule = new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node): scala.xml.NodeSeq =
        if (n.label == "dependency" && (n \ "scope").text == "provided" && (n \ "groupId").text == "org.scoverage")
          scala.xml.NodeSeq.Empty
        else n
    }
    val transformer = new scala.xml.transform.RuleTransformer(rewriteRule)
    transformer.transform(node).head
  },
  releaseProcess := List[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishSignedArtifacts,
    releaseToCentral,
    setNextVersion,
    commitNextVersion,
    pushChanges,
    publishArtifacts
  ))


// ====================================================================

lazy val maintainer = settingKey[String]("Maintainer")
lazy val githubUser = settingKey[String]("Github username")
lazy val githubRepo = settingKey[String]("Github repository")

lazy val publishSignedArtifacts = publishArtifacts.copy(
  action = { state =>
    val extracted = Project extract state
    val ref = extracted get thisProjectRef
    extracted.runAggregated(publishSigned in Global in ref, state)
  },
  enableCrossBuild = true)

lazy val releaseToCentral = ReleaseStep({ state =>
  val extracted = Project extract state
  val ref = extracted get thisProjectRef
  extracted.runAggregated(sonatypeReleaseAll in Global in ref, state)
}, enableCrossBuild = true)

addCommandAlias("travis", ";clean;coverage;test;coverageReport;coverageAggregate")
