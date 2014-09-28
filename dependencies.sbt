libraryDependencies ++= {
  object Version {
    val commonsLang = "3.3.2"
    val compress    = "1.8.1"
    val jena        = "2.12.0"
    val logback     = "1.1.2"
    val metrics     = "3.0.2"
    val slf4j       = "1.7.7"
    val scalatest   = "2.2.2"
    val scalacheck  = "1.11.5"
  }
  object Library {
    val commonsLang = "org.apache.commons"   % "commons-lang3"    % Version.commonsLang
    val compress    = "org.apache.commons"   % "commons-compress" % Version.compress
    val jena        = "org.apache.jena"      % "jena-core"        % Version.jena
    val logback     = "ch.qos.logback"       % "logback-classic"  % Version.logback
    val metrics     = "com.codahale.metrics" % "metrics-core"     % Version.metrics   exclude("org.slf4j", "slf4j-api")
    val slf4j       = "org.slf4j"            % "slf4j-api"        % Version.slf4j
    val scalatest   = "org.scalatest"       %% "scalatest"        % Version.scalatest
    val scalacheck  = "org.scalacheck"      %% "scalacheck"       % Version.scalacheck
  }
  List(
    Library.compress    % "compile",
    Library.slf4j       % "compile",
    Library.scalatest   % "test",
    Library.scalacheck  % "test",
    Library.jena        % "test",
    Library.commonsLang % "test",
    Library.logback     % "test",
    Library.metrics     % "test"
  )
}
