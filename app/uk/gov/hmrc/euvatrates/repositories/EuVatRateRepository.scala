/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.euvatrates.repositories

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, Indexes, IndexModel, IndexOptions, ReplaceOptions}
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent

import java.time.{Clock, LocalDate}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatRateRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     appConfig: AppConfig,
                                     clock: Clock
                                   )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[EuVatRate](
    collectionName = "eu-vat-rates",
    mongoComponent = mongoComponent,
    domainFormat = EuVatRate.format,
    replaceIndexes = true,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("situatedOn"),
        IndexOptions()
          .name("situatedOnIdx")
          .unique(false)
      )
    )
  ) {

  private def byUniqueEntry(country: Country, vatRate: BigDecimal, situatedOn: LocalDate): Bson =
    Filters.and(
      Filters.equal("country", country),
      Filters.equal("vatRate", vatRate),
      Filters.equal("situatedOn", situatedOn)
    )

  def get(country: Country, fromDate: LocalDate) = {
    collection.find(
        Filters.and(
          Filters.equal("country", country),
          Filters.gte("situatedOn", fromDate)
        )
      )
      .toFuture()
  }

  def set(euVatRate: EuVatRate): Future[EuVatRate] = {
    collection
      .replaceOne(
        filter = byUniqueEntry(euVatRate.country, euVatRate.vatRate, euVatRate.situatedOn),
        replacement = euVatRate,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => euVatRate)
  }

}
