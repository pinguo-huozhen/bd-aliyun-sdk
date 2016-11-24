import sbt.Keys._

compileOrder in ThisBuild := CompileOrder.JavaThenScala

name := "bd-aliyun-sdk"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= dependencies.apache
libraryDependencies ++= dependencies.config
libraryDependencies ++= dependencies.json

lazy val dependencies = new {

  val apache = Seq(
    "commons-codec" % "commons-codec" % "1.4",
    "org.apache.httpcomponents" % "httpclient" % "4.5.2",
    "org.apache.directory.studio" % "org.apache.commons.io" % "2.4"
  )

  val json = Seq(
    "org.json4s" %% "json4s-jackson" % "3.2.11",
    "org.json4s" %% "json4s-ext" % "3.2.11"
  ) map (_ withSources())

  val config = Seq(
    "com.typesafe" % "config" % "1.3.1"
  ) map (_ withSources())

}
    