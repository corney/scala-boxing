name := "Scala-boxing"
version := "1.0"
scalaVersion := "2.13.12"

lazy val asyncHome = sys
  .env
  .getOrElse(
    "ASYNC_PROFILER_HOME",
    "ASYNC_PROFILER_HOME_env_not_specified",
  )

def commonJmhSettings(moduleName: String) =
  Seq(
    name := moduleName,
    Jmh / javaOptions += s"-Djava.library.path=$asyncHome/lib",
    Jmh / sourceDirectory :=
      (Test / sourceDirectory).value,
    Jmh / classDirectory :=
      (Test / classDirectory).value,
    Jmh / dependencyClasspath :=
      (Test / dependencyClasspath).value,
    Jmh / compile :=
      (Jmh / compile).dependsOn(Test / compile).value,
    Jmh / run :=
      (Jmh / run).dependsOn(Jmh / compile).evaluated,
  )

lazy val root = (project in file("."))
.enablePlugins(JmhPlugin)
  .settings(commonJmhSettings("scalaBoxing"))
