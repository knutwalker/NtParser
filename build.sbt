organization in ThisBuild := "de.knutwalker"
homepage in ThisBuild := Some(url("https://github.com/knutwalker/ntparser"))
licenses in ThisBuild += "Apache License, Verison 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/knutwalker/ntparser"), "scm:git:https://github.com/knutwalker/ntparser.git", Some("scm:git:ssh://git@github.com:knutwalker/ntparser.git")))
startYear in ThisBuild := Some(2014)

scalacOptions in ThisBuild ++= List(
  "-deprecation",
  "-encoding", "UTF-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-optimise",
  "-unchecked",
  "-Xcheckinit",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Yclosure-elim",
  "-Ydead-code",
  "-Yinline",
  "-Yinline-handlers",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-adapted-args",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen"
)

publishMavenStyle in ThisBuild := true
publishArtifact in ThisBuild in Test := false
pomIncludeRepository in ThisBuild := { _ => false }

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra in ThisBuild :=
  <developers>
    <developer>
      <id>knutwalker</id>
      <name>Paul Horn</name>
      <url>http://knutwalker.de/</url>
    </developer>
  </developers>

SonatypeKeys.profileName in ThisBuild := "knutwalker"
