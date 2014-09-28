import JmhKeys._

jmhSettings

outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}"

resolvers += "NxParser" at "http://nxparser.googlecode.com/svn/repository"
libraryDependencies ++= List(
  "org.apache.jena"      % "jena-core" % "2.12.0",
  "org.semanticweb.yars" % "nxparser"  % "1.2.6"
)
