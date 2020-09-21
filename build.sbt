inThisBuild(
  List(
    organization := "com.github.mkobzik",
    version := "0.1",
    scalaVersion := "2.13.3"
  )
)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "sensor-statistics-cli",
    scalacOptions -= "-Xfatal-warnings",
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.2.0",
      "org.typelevel" %% "cats-effect" % "2.2.0",
      "org.typelevel" %% "cats-tagless-macros" % "0.11",
      "dev.profunktor" %% "console4cats" % "0.8.1",
      "co.fs2" %% "fs2-core" % "2.4.4",
      "co.fs2" %% "fs2-io" % "2.4.4",
      "io.estatico" %% "newtype" % "0.4.4",
      "com.monovore" %% "decline-effect" % "1.3.0",
      "com.github.julien-truffaut" %% "monocle-core"  % "2.0.3",
      "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3",
      "eu.timepit" %% "refined" % "0.9.15",
      "com.lihaoyi" %% "fastparse" % "2.2.2",
      "org.scalameta" %% "munit" % "0.7.12" % Test
    ),
    assemblyJarName := name.value + ".jar",
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion)
  )
