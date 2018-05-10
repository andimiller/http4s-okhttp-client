name := "http4s-okhttp-client"

version := "0.1"

scalaVersion := "2.12.6"

lazy val http4sVersion = "0.18.10"

lazy val fs2ReactiveStreams               = "com.github.zainab-ali"  %% "fs2-reactive-streams"      % "0.5.1"

libraryDependencies += "org.http4s" %% "http4s-core" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-client" % http4sVersion
libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.10.0"
libraryDependencies += fs2ReactiveStreams
