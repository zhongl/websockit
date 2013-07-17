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
                                                            libraryDependencies ++= compileDependencies ++ testDependencies
                                                          )

  lazy val compileDependencies = Seq(
                                      "io.netty" % "netty-codec-http" % "4.0.1.Final",
                                      "com.jayway.jsonpath" % "json-path" % "0.8.1",
                                      "ch.qos.logback" % "logback-classic" % "1.0.13",
                                      "com.twitter" %% "util-collection" % "6.3.7"
                                    )

  lazy val testDependencies = Seq(
                                   "org.scalatest" % "scalatest_2.10" % "1.9.1"
                                 ) map { _ % "test" }
}
