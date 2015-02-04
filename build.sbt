name := """calendar"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

// only for Play 2.3.x
libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"