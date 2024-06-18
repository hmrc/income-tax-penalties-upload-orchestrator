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

import base.SpecBase
import models.SDESNotificationRecord
import models.notification.{RecordStatusEnum, SDESAudit, SDESChecksum, SDESNotification, SDESNotificationFile, SDESProperties}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import repositories.FileNotificationRepository

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class MongoQueryControllerSpec extends SpecBase {
  val mockRepo: FileNotificationRepository = mock[FileNotificationRepository]
  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]

  class Setup {
    reset(mockRepo)
    val controller = new MongoQueryController(stubControllerComponents(), mockRepo)
  }

  "getNumberOfRecords" should {
    s"return OK (${Status.OK}) with correct number of records" in new Setup {
      when(mockRepo.countAllRecords()).thenReturn(Future.successful(5))
      val result: Future[Result] = controller.getNumberOfRecords()(fakeRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "5"
    }
  }

  "getNotificationsInState" should {
    s"return OK (${Status.OK}) with requested records" in new Setup {
      val filterValues = RecordStatusEnum.values.toSeq
      when(mockRepo.getNotificationsInState(filterValues:_*)).thenReturn(Future.successful(Seq(
        SDESNotificationRecord(
          reference = "57d34e2b-36fe-0399-8a9c-efcfc5aa2a93",
          createdAt = LocalDateTime.of(2020,1,1,1,1).toInstant(ZoneOffset.UTC),
          updatedAt = LocalDateTime.of(2020,2,2,2,2).toInstant(ZoneOffset.UTC),
          nextAttemptAt = LocalDateTime.of(2020,3,3,3,3).toInstant(ZoneOffset.UTC),
          notification = SDESNotification(
          informationType = "foo",
          file = SDESNotificationFile(
            recipientOrSender = "recipient1",
            name = "file1.txt",
            location = "http://example.com/file1.txt",
            checksum = SDESChecksum(algorithm = "SHA-256", value = "check123"),
            size = 1,
            properties = Seq(SDESProperties("name", "value"))),
          audit = SDESAudit("correlationID")
        ))
      )))
      val result: Future[Result] = controller.getNotificationsInState(filterValues:_*)(fakeRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe """[{"reference":"57d34e2b-36fe-0399-8a9c-efcfc5aa2a93","status":"PENDING","numberOfAttempts":0,"createdAt":{"$date":{"$numberLong":"1577840460000"}},"updatedAt":{"$date":{"$numberLong":"1580608920000"}},"nextAttemptAt":{"$date":{"$numberLong":"1583204580000"}},"notification":{"informationType":"foo","file":{"recipientOrSender":"recipient1","name":"file1.txt","location":"http://example.com/file1.txt","checksum":{"algorithm":"SHA-256","value":"check123"},"size":1,"properties":[{"name":"name","value":"value"}]},"audit":{"correlationID":"correlationID"}}}]"""
    }
  }
}
