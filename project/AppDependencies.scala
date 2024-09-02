/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.*

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "9.0.0"
  private val hmrcMongoVersion = "2.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-backend-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
    "io.github.samueleresca"  %% "pekko-quartz-scheduler"           % "1.2.0-pekko-1.0.x",
    "org.quartz-scheduler"    % "quartz"                            % "2.3.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-test-$playVersion"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test-$playVersion"    % hmrcMongoVersion            % Test
  )

  val it: Seq[Nothing] = Seq(
  )
}
