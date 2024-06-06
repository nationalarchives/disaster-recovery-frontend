import Dependencies.*

val Http4sVersion = "1.0.0-M29"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"

lazy val root = (project in file(".")).aggregate(builder, webapp)


lazy val utils = (project in file("utils"))
  .settings(
    name := "disaster-recovery-utils",
    scalaVersion := "3.4.0"
  )

lazy val builder = (project in file("builder"))
  .settings(
    name := "disaster-recovery-database-builder",
    scalaVersion := "3.4.0",
    version := "0.0.1-SNAPSHOT",
    assembly / assemblyJarName := "builder.jar",
    libraryDependencies ++= Seq(
      ocfl,
      fs2,
      sqsClient,
      scalaXml,
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.xerial" % "sqlite-jdbc" % "3.46.0.0",
    )
  )
  .settings(commonSettings)
  .dependsOn(utils)

lazy val webapp = (project in file("webapp"))
  .enablePlugins(SbtTwirl)
  .settings(commonSettings)
  .settings(
    organization := "com.example",
    assembly / assemblyJarName := "webapp.jar",
    name := "disaster-recovery-frontend-spike",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.4.0",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.xerial" % "sqlite-jdbc" % "3.46.0.0",
      ocfl
    ),
    testFrameworks += new TestFramework("munit.Framework")
  ).dependsOn(utils)

scalacOptions ++= Seq("-Wunused:imports", "-Werror")


lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    log4jSlf4j,
    log4jCore,
    log4jTemplateJson,
    log4CatsSlf4j,
    log4Cats,
    pureConfigCatsEffect,
    pureConfig,
  ),
  (assembly / assemblyMergeStrategy) := {
    case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => sbtassembly.Log4j2MergeStrategy.plugincache
    case PathList("META-INF", xs@_*) =>
      xs map {
        _.toLowerCase
      } match {
        case "services" :: xs =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.discard
      }
    case manifest if manifest.contains("MANIFEST.MF") => MergeStrategy.discard
    case x => MergeStrategy.last
  }
)