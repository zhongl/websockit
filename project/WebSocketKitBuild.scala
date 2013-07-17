import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform.scalariformSettings

import scalariform.formatter.preferences._

import com.typesafe.sbt.SbtScalariform._


object WebsocketkitBuild extends Build {

  lazy val kit = Project(
    id = "kit",
    base = file("."),
    settings = commonSettings ++ (name := "websocket-kit")
  )

  lazy val commonSettings = Project.defaultSettings ++ scalariformSettings ++ Seq(
    organization := "me.zhongl",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.0",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    libraryDependencies ++= compileDependencies ++ testDependencies,
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  )

  lazy val compileDependencies = Seq(
    "io.netty" % "netty-codec-http" % "4.0.1.Final",
    "com.jayway.jsonpath" % "json-path" % "0.8.1",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.twitter" %% "util-eval" % "6.3.7"
  )

  lazy val testDependencies = Seq(
    "org.scalatest" % "scalatest_2.10" % "1.9.1"
  ) map { _ % "test" }
}
