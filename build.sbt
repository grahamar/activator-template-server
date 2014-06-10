name := """activator-template-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.activator" % "activator-templates-cache" % "1.0-5b61a82baa9bb0d9fe161752d6721fe6551eed54",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.3.2.201404171909-r",
  "org.eclipse.jgit" % "org.eclipse.jgit.archive" % "3.3.2.201404171909-r"
)
