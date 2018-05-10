name := "http4s-okhttp-client"

version := "0.1"

scalaVersion := "2.12.6"

lazy val http4sVersion = "0.18.10"

libraryDependencies += "org.http4s" %% "http4s-core" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-client" % http4sVersion
libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.10.0"
libraryDependencies += "org.mortbay.jetty.alpn" % "alpn-boot" % "8.1.12.v20180117"

fork in run := true

javaOptions in run ++= {
  for {
    file <- (managedClasspath in Runtime).value.map(_.data)
    path = file.getAbsolutePath if path.contains("jetty.alpn")
  } yield "-Xbootclasspath/p:" + path
}

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"