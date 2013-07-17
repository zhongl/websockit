import sbt._
import sbt.Keys._

object WebsocketkitBuild extends Build {

  lazy val kit = Project(
    id = "kit",
    base = file("."),
    settings = commonSettings ++ (name := "websocket-kit")
  )

  lazy val commonSettings = Project.defaultSettings ++ Seq(
    organization := "me.zhongl",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.0",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    libraryDependencies ++= compileDependencies ++ testDependencies
  )

  lazy val compileDependencies = Seq(
    "io.netty" % "netty-codec-http" % "4.0.1.Final",
    "org.json4s" %% "json4s-native" % "3.2.2",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.twitter" %% "util-collection" % "6.3.7"
  )

  lazy val testDependencies = Seq(
    "org.scalatest" % "scalatest_2.10" % "1.9.1"
  ) map { _ % "test" }
}
