import sbt._
import sbt.Keys._

object WebsocketkitBuild extends Build {

  lazy val root = Project(id = "root", base = file(".")) aggregate(stub, driver)

  lazy val stub = Project(
    id = "stub",
    base = file("stub"),
    settings = commonSettings ++ (name := "websocket-kit-stub")
  )

  lazy val driver = Project(
    id = "driver",
    base = file("driver"),
    settings = commonSettings ++ (name := "websocket-kit-driver")
  )

  lazy val commonSettings = Project.defaultSettings ++ Seq(
    organization := "me.zhongl",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.9.2",
    libraryDependencies ++= compileDependencies ++ testDependencies
  )

  lazy val compileDependencies = Seq(
    "io.netty" % "netty-codec-http" % "4.0.1.Final",
    "com.jayway.jsonpath" % "json-path" % "0.8.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13"
  )

  lazy val testDependencies = Seq(
    "org.scalatest" % "scalatest_2.9.2" % "1.9.1"
  ) map { _ % "test" }
}
