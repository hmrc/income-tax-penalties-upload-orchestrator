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

package controllers.testOnly

import models.notification.RecordStatusEnum
import play.api.libs.json.Json

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.FileNotificationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class MongoQueryController @Inject()(cc: ControllerComponents,
                                     repository: FileNotificationRepository)(implicit val ec: ExecutionContext) extends BackendController(cc) {

  def getNumberOfRecords: Action[AnyContent] = Action.async {
    repository.countAllRecords().map {
        numberOfRecords => {
          Ok(s"$numberOfRecords")
        }
    }
  }

  def getNotificationsInState(state: RecordStatusEnum.Value*): Action[AnyContent] = Action.async {
    repository.getNotificationsInState(state:_*).map {
      result => {
        Ok(Json.toJson(result))
      }
    }
  }
}
