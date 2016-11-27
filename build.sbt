lazy val `bd-aliyun-sdk` = (project in file("."))
  .settings(
    name := "bd-aliyun-sdk",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec" % "1.4",
      "org.apache.httpcomponents" % "httpclient" % "4.5.2",
      "org.apache.directory.studio" % "org.apache.commons.io" % "2.4",
      "org.json4s" %% "json4s-jackson" % "3.2.11",
      "org.json4s" %% "json4s-ext" % "3.2.11",
      "com.typesafe" % "config" % "1.3.1",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2" withSources()
    )
  )
    