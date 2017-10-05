name := "Kotlin-ScalaJS"
organization := "ch.epfl.k2sjs"

publishMavenStyle := true

version := "0.1"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "org.jetbrains.kotlin" % "kotlin-compiler" % "1.1.1",
  "org.scala-js" %% "scalajs-ir" % "1.0.0-M1",
  "org.scala-js" %% "scalajs-tools" % "1.0.0-M1" % Test,
  "com.github.scopt" %% "scopt" % "3.5.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

parallelExecution in Test := false
