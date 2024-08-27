import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.6.0"
  private val hmrcMongoVersion = "1.9.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"     % "1.2.1-pekko-1.0.x",
    "org.quartz-scheduler"    % "quartz" % "2.3.2"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
    //"org.mockito"             % "mockito-all"                 % "1.10.19"                   % Test
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.31"                   % Test
  )

  val it = Seq(
//    "org.mockito"             % "mockito-all"                 % "1.10.19"                   % Test
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.31"                   % Test
  )
}
