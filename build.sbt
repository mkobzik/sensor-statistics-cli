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
      "org.tpolecat" %% "atto-core" % "0.8.0",
      "org.scalameta" %% "munit" % "0.7.12" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion)
  )
