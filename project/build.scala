/*
 * Copyright 2014 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._
import xerial.sbt.Sonatype.SonatypeKeys._


object NtParserBuild extends Build {

  lazy val core =
    Project("core", file("core")).settings(ntparserSettings: _*)

  lazy val tests =
    Project("tests", file("tests")).settings(noPublishSettings: _*).dependsOn(core)

  lazy val benchmark =
    Project("benchmark", file("benchmark")).settings(noPublishSettings: _*).dependsOn(core)

  lazy val parent =
    Project("parent", file(".")).settings(noPublishSettings: _*).aggregate(core, tests, benchmark).dependsOn(core, tests, benchmark)

  lazy val ntparserSettings = Defaults.defaultSettings ++ signedReleaseSettings

  lazy val noPublishSettings = ntparserSettings ++ List(
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

  lazy val signedReleaseSettings = releaseSettings ++ List(
    releaseProcess := List[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      runClean,
      runTest,
      commitReleaseVersion,
      tagRelease,
      publishSignedArtifacts,
      releaseToCentral,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      publishArtifacts
    ),
    tagComment <<= (Keys.version in ThisBuild) map (v => s"Release version $v"),
    commitMessage <<= (Keys.version in ThisBuild) map (v => s"Set version to $v"),
    versionBump := sbtrelease.Version.Bump.Bugfix
  )

  lazy val publishSignedArtifacts = publishArtifacts.copy(
    action = { st: State =>
      val extracted = Project.extract(st)
      val ref = extracted.get(Keys.thisProjectRef)
      extracted.runAggregated(publishSigned in Global in ref, st)
    },
    enableCrossBuild = true
  )

  lazy val releaseToCentral = ReleaseStep(
    action = { st: State =>
      val extracted = Project.extract(st)
      val ref = extracted.get(Keys.thisProjectRef)
      extracted.runAggregated(sonatypeReleaseAll in Global in ref, st)
    },
    enableCrossBuild = true
  )
}
