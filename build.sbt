import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.3"

// maxErrors := 1

lazy val microservice = Project("income-tax-penalties-upload-orchestrator", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
//    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions -= "-deprecation", //XXX: remove when Scala 3.3.4
    scalacOptions -= "-unchecked", //XXX: remove when Scala 3.3.4
    scalacOptions -= "-encoding", //XXX: remove when Scala 3.3.4
    PlayKeys.playDefaultPort := 9188,
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    ScoverageKeys.coverageExcludedPackages := "controllers.testOnly.*",
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;..*components.*;" +
      ".*Routes.*;.*ControllerConfiguration;.*Modules;",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true)
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")))

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.it,
    scalacOptions -= "-deprecation", //XXX: remove when Scala 3.3.4
    scalacOptions -= "-unchecked", //XXX: remove when Scala 3.3.4
    scalacOptions -= "-encoding", //XXX: remove when Scala 3.3.4
  )
  .settings(scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")))
