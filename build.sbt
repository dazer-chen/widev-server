name := """widev-server-v2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Rhinofly Internal Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local"
)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "jp.t2v" %% "stackable-controller" % "0.4.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.7.2",
  "joda-time" %  "joda-time" % "2.1",
  "org.joda" % "joda-convert" % "1.2",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "jp.t2v" %% "play2-auth" % "0.12.0",
  "jp.t2v" %% "play2-auth-test" % "0.12.0" % "test",
  "nl.rhinofly" %% "play-s3" % "5.0.0"
)
