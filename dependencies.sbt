libraryDependencies ++= {
  object Version {
    val commonsLang = "3.3.2"
    val compress    = "1.8.1"
    val metrics     = "3.0.2"
    val slf4j       = "1.7.7"
    val scalatest   = "2.2.2"
    val scalacheck  = "1.11.5"
  }
  object Library {
    val commonsLang = "org.apache.commons"   % "commons-lang3"    % Version.commonsLang
    val compress    = "org.apache.commons"   % "commons-compress" % Version.compress
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
    Library.commonsLang % "test",
    Library.metrics     % "test"
  )
}
