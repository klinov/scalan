import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtrelease.ReleasePlugin._

object ScalanBuild extends Build {

  val opts = scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-feature",
    "-Ywarn-adapted-args",
    "-Ywarn-inaccessible",
    "-Ywarn-nullary-override",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:experimental.macros")

  val commonDeps = libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
    "com.github.axel22" %% "scalameter" % "0.5-M2" % "test",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "ch.qos.logback" % "logback-classic" % "1.1.2")

  val testSettings = inConfig(ItTest)(Defaults.testTasks) ++
    inConfig(PerfTest)(Defaults.testTasks) ++ Seq(
    testOptions in Test := Seq(Tests.Filter(x => !itFilter(x))),
    testOptions in ItTest := Seq(Tests.Filter(x => itFilter(x))),
    testFrameworks in PerfTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),
    logBuffered in PerfTest := false,
    // needed thanks to http://stackoverflow.com/questions/7898273/how-to-get-logging-working-in-scala-unit-tests-with-testng-slf4s-and-logback
    parallelExecution in Test := false,
    parallelExecution in ItTest := false,
    parallelExecution in PerfTest := false,
    fork in PerfTest := true,
    javaOptions in PerfTest ++= Seq("-Xmx30G", "-Xms15G"),
    publishArtifact in Test := true,
    publishArtifact in(Test, packageDoc) := false,
    test in assembly := {})

  val crossCompilation =
    crossScalaVersions := Seq("2.10.4", "2.11.4")

  val commonSettings = Seq(
    scalaVersion := "2.10.4",
    organization := "com.huawei.scalan",
    publishTo := {
      val nexus = "http://10.122.85.37:9081/nexus/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at (nexus + "content/repositories/snapshots"))
      else
        Some("releases" at (nexus + "content/repositories/releases"))
    },
    opts, commonDeps) ++ testSettings ++ releaseSettings

  lazy val noPublishingSettings = Seq(
    publishArtifact := false,
    publishTo := None)

  lazy val ItTest = config("it").extend(Test)

  lazy val PerfTest = config("perf").extend(Test)

  def itFilter(name: String): Boolean = name.contains("ItTests")

  lazy val `compile->compile;test->test` = "compile->compile;test->test"

  implicit class ProjectExt(p: Project) {
    def withTestConfigsAndCommonSettings =
      p.configs(ItTest, PerfTest).settings(commonSettings: _*)
  }

  lazy val common = project.withTestConfigsAndCommonSettings
    .settings(crossCompilation)

  lazy val meta = project.dependsOn(common).withTestConfigsAndCommonSettings
    .settings(
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      fork in Test := true,
      fork in ItTest := true)

  lazy val core = project.dependsOn(common).withTestConfigsAndCommonSettings
    .settings(crossCompilation)
    .settings(
      libraryDependencies ++= Seq(
        "com.chuusai" % "shapeless" % "2.0.0" cross CrossVersion.binaryMapped {
          case "2.10" => scalaVersion.value
          case v => v
        },
        "cglib" % "cglib" % "3.1",
        "org.objenesis" % "objenesis" % "2.1"))

  lazy val frontend = project.dependsOn(core % `compile->compile;test->test`, common).withTestConfigsAndCommonSettings
    .settings(
      scalaVersion := "2.11.4",
      crossScalaVersions := Seq("2.11.4"),
      libraryDependencies += "ch.epfl.lamp" %% "scala-yinyang" % "0.1.0")

  lazy val ce = Project("community-edition", file("community-edition"))
    .dependsOn(core % `compile->compile;test->test`)
    .withTestConfigsAndCommonSettings

  val virtScala = Option(System.getenv("SCALA_VIRTUALIZED_VERSION")).getOrElse("2.10.2")

  val lms = "EPFL" % "lms_local_2.10" % "0.3-SNAPSHOT"

  lazy val lmsBackend = Project("lms-backend", file("lms-backend"))
    .dependsOn(core % `compile->compile;test->test`, ce % `compile->compile;test->test`)
    .withTestConfigsAndCommonSettings
    .settings(
      libraryDependencies ++= Seq(lms,
        lms classifier "tests",
        "org.scala-lang.virtualized" % "scala-library" % virtScala,
        "org.scala-lang.virtualized" % "scala-compiler" % virtScala),
      scalaOrganization := "org.scala-lang.virtualized",
      scalaVersion := virtScala,
      // we know we use LMS snapshot here, ignore it
      ReleaseKeys.snapshotDependencies := Seq.empty)

  // name to make this the default project
  lazy val root = Project("scalan", file("."))
    .aggregate(common, meta, core, ce, lmsBackend)
    .settings(noPublishingSettings: _*)
}
