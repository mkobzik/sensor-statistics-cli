ThisBuild / organization := "com.github.mkobzik"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.3"

lazy val root = (project in file("."))
  .settings(
    name := "sensor-statistics-cli",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(

    )
  )
