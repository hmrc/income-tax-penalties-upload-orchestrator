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

package repositories

import com.mongodb.client.model.Updates.{combine, set}
import config.AppConfig
import crypto.CryptoProvider
import models.SDESNotificationRecord
import models.notification.RecordStatusEnum
import models.notification.RecordStatusEnum._
import org.mongodb.scala.model.Filters.{equal, in}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.inc
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.libs.json.{Format, Json, OFormat}
import repositories.FileNotificationRepository.{inProgressStatuses, toCamlCase}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.Logger.logger
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.{PagerDutyHelper, TimeMachine}

import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.ObservableFuture

object FileNotificationRepository {
  val inProgressStatuses: Seq[String] = RecordStatusEnum.values.filterNot(_==FILE_PROCESSED_IN_SDES).map(_.toString).toList
  def toCamlCase(s: String): String = s.split("_", -1).zipWithIndex.map{case (s,i)=> if (i>0) s.capitalize else s}.mkString
}

@Singleton
class FileNotificationRepository @Inject()(mongoComponent: MongoComponent,
                                           timeMachine: TimeMachine,
                                           appConfig: AppConfig)(implicit ec: ExecutionContext, cryptoProvider: CryptoProvider)
  extends PlayMongoRepository[SDESNotificationRecord](
    collectionName = "sdes-file-notifications",
    mongoComponent = mongoComponent,
    domainFormat = SDESNotificationRecord.mongoFormats,
    indexes = Seq(
      IndexModel(ascending("reference"), IndexOptions().unique(true)),
      IndexModel(ascending("status")),
      IndexModel(ascending("updatedAt"), IndexOptions().name("fileProcessedInSdesUpdatedAtIndex")
        .partialFilterExpression(equal("status", FILE_PROCESSED_IN_SDES.toString))
        .expireAfter(appConfig.completedTtl, TimeUnit.DAYS)),
    ) ++ inProgressStatuses.map { status =>
      IndexModel(ascending("updatedAt"), IndexOptions().name(toCamlCase(s"${status.toLowerCase}_updated_at_index"))
        .partialFilterExpression(equal("status", status))
        .expireAfter(appConfig.inProgressTtl, TimeUnit.DAYS))
    }
  ) with MongoJavatimeFormats {

  private implicit val crypto: Encrypter with Decrypter = cryptoProvider.getCrypto
  implicit val dateFormat: Format[Instant] = instantFormat
  implicit val mongoFormats: OFormat[SDESNotificationRecord] = Json.format[SDESNotificationRecord]

  def insertFileNotifications(records: Seq[SDESNotificationRecord]): Future[Boolean] = {
    val encryptedFileNotifications = records.map(record => SDESNotificationRecord.encrypt(record))
    collection.insertMany(encryptedFileNotifications).toFuture().map(_.wasAcknowledged())
      .recover {
        case e =>
          PagerDutyHelper.log("insertFileNotifications", FAILED_TO_INSERT_SDES_NOTIFICATION)
          logger.error(s"[FileNotificationRepository][insertFileNotifications] - Failed to insert SDES notification with message: ${e.getMessage}")
          false
      }
  }

  def updateFileNotification(record: SDESNotificationRecord): Future[SDESNotificationRecord] = {
    val encryptedFileNotifications = SDESNotificationRecord.encrypt(record)
    logger.info(s"[FileNotificationRepository][updateFileNotification] - Updating record ${record.reference} in Mongo")
    collection.findOneAndUpdate(equal("reference", encryptedFileNotifications.reference), combine(
      set("nextAttemptAt", Codecs.toBson(encryptedFileNotifications.nextAttemptAt)),
      set("status", encryptedFileNotifications.status.toString),
      set("numberOfAttempts", encryptedFileNotifications.numberOfAttempts),
      set("updatedAt", Codecs.toBson(encryptedFileNotifications.updatedAt))
    )).toFuture()
  }

  def updateFileNotification(reference: String, updatedStatus: RecordStatusEnum.Value): Future[SDESNotificationRecord] = {
    val result = collection.find(equal("reference", reference)).toFuture()
    result.flatMap { records =>
      if(records.nonEmpty && records.head.status.equals(FILE_PROCESSED_IN_SDES)) {
        logger.info(s"[FileNotificationRepository][updateFileNotification] - Record $reference already processed skipping update")
        Future(records.head)
      } else {
        logger.info(s"[FileNotificationRepository][updateFileNotification] - Updating record $reference in Mongo")
        collection.findOneAndUpdate(equal("reference", reference), combine(
          if (updatedStatus == NOT_PROCESSED_PENDING_RETRY || updatedStatus == FAILED_PENDING_RETRY || updatedStatus == FILE_NOT_RECEIVED_IN_SDES_PENDING_RETRY)
            set("nextAttemptAt", Codecs.toBson(timeMachine.now.plus(appConfig.minutesUntilNextAttemptOnCallbackFailure, MINUTES)))
          else set("nextAttemptAt", Codecs.toBson(timeMachine.now)),
          set("status", updatedStatus.toString),
          inc("numberOfAttempts", if (updatedStatus == NOT_PROCESSED_PENDING_RETRY || updatedStatus == FAILED_PENDING_RETRY || updatedStatus == FILE_NOT_RECEIVED_IN_SDES_PENDING_RETRY) 1 else 0),
          set("updatedAt", Codecs.toBson(timeMachine.now))
        )).toFuture()
      }
    }
  }

  def getPendingNotifications: Future[Seq[SDESNotificationRecord]] = {
    getNotificationsInState(PENDING, NOT_PROCESSED_PENDING_RETRY, FAILED_PENDING_RETRY, FILE_NOT_RECEIVED_IN_SDES_PENDING_RETRY)
  }

  def getNotificationsInState(state: RecordStatusEnum.Value*): Future[Seq[SDESNotificationRecord]] = {
    collection.find(withStatus(state:_*)).map(SDESNotificationRecord.decrypt(_)).toFuture()
  }

  def countRecordsByStatus(status: RecordStatusEnum.Value*): Future[Long] = {
    collection.countDocuments(withStatus(status:_*)).toFuture()
  }

  def countAllRecords(): Future[Long] = {
    collection.countDocuments().toFuture()
  }

  private def withStatus(state: RecordStatusEnum.Value*) = in("status", state.map(_.toString):_*)

}
