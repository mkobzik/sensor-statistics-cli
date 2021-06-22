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
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.typelevel" %% "cats-effect" % "3.1.1", 
      "org.typelevel" %% "cats-tagless-macros" % "0.14.0",
      "co.fs2" %% "fs2-core" % "3.0.4",
      "co.fs2" %% "fs2-io" % "3.0.4",
      "io.estatico" %% "newtype" % "0.4.4",
      "com.monovore" %% "decline-effect" % "2.0.0-RC1",
      "com.github.julien-truffaut" %% "monocle-core"  % "2.0.3",
      "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3",
      "eu.timepit" %% "refined" % "0.9.26",
      "com.lihaoyi" %% "fastparse" % "2.2.2",
      "org.scalameta" %% "munit" % "0.7.26" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % Test
    ),
    assemblyJarName := name.value + ".jar",
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion)
  )
