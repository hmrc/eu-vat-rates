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
import org.mongodb.scala.model._
import org.mongodb.scala.ObservableFuture
import play.api.libs.json.Format
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EuVatRateRepository @Inject()(
                                     mongoComponent: MongoComponent,
                                     appConfig: AppConfig
                                   )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[EuVatRate](
    collectionName = "eu-vat-rates",
    mongoComponent = mongoComponent,
    domainFormat = EuVatRate.dbFormat,
    replaceIndexes = true,
    extraCodecs = Seq(BigDecimalCodec),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("countryCode", "vatRate", "situatedOn", "dateFrom", "dateTo"),
        IndexOptions()
          .name("euVatRateReferenceIdx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .unique(false)
          .expireAfter(appConfig.cacheTtl, TimeUnit.DAYS)
      )
    )
  ) {

  implicit val instantFormat: Format[LocalDate] = MongoJavatimeFormats.localDateFormat

  private def byUniqueEntry(
                             country: Country,
                             vatRate: BigDecimal,
                             situatedOn: LocalDate,
                             dateFrom: LocalDate,
                             dateTo: LocalDate
                           ): Bson =
    Filters.and(
      Filters.equal("countryCode", toBson(country.code)),
      Filters.equal("vatRate", toBson(vatRate)),
      Filters.equal("situatedOn", toBson(situatedOn)),
      Filters.equal("dateFrom", toBson(dateFrom)),
      Filters.equal("dateTo", toBson(dateTo)),
    )

  def get(country: Country, fromDate: LocalDate, toDate: LocalDate): Future[Seq[EuVatRate]] = {
    collection.find(
        Filters.or(
          Filters.and(
            Filters.equal("countryCode", toBson(country.code)),
            Filters.lte("dateFrom", toBson(fromDate)),
            Filters.gte("dateTo", toBson(fromDate))
          ),
          Filters.and(
            Filters.equal("countryCode", toBson(country.code)),
            Filters.lte("dateFrom", toBson(toDate)),
            Filters.gte("dateTo", toBson(toDate))
          )

        )
      )
      .toFuture()
  }

  def getMany(countries: Seq[Country], fromDate: LocalDate, toDate: LocalDate): Future[Seq[EuVatRate]] = {
    val countryCodes = countries.map(country => toBson(country.code))
    collection.find(
        Filters.or(
          Filters.and(
            Filters.in("countryCode", countryCodes: _*),
            Filters.lte("dateFrom", toBson(fromDate)),
            Filters.gte("dateTo", toBson(fromDate))
          ),
          Filters.and(
            Filters.in("countryCode", countryCodes: _*),
            Filters.lte("dateFrom", toBson(toDate)),
            Filters.gte("dateTo", toBson(toDate))
          ),
          Filters.and(
            Filters.in("countryCode", countryCodes: _*),
            Filters.gte("dateFrom", toBson(fromDate)),
            Filters.lte("dateTo", toBson(toDate))
          )

        )
      )
      .toFuture()
  }

  def set(euVatRate: EuVatRate): Future[EuVatRate] = {
    collection
      .replaceOne(
        filter = byUniqueEntry(euVatRate.country, euVatRate.vatRate, euVatRate.situatedOn, euVatRate.dateFrom, euVatRate.dateTo),
        replacement = euVatRate,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => euVatRate)
  }

  def setMany(euVatRates: Seq[EuVatRate]): Future[Seq[EuVatRate]] = {
    collection
      .bulkWrite(euVatRates.map { euVatRate =>
        ReplaceOneModel(
          filter = byUniqueEntry(euVatRate.country, euVatRate.vatRate, euVatRate.situatedOn, euVatRate.dateFrom, euVatRate.dateTo),
          replacement = euVatRate,
          replaceOptions = ReplaceOptions().upsert(true)
        )
      }, BulkWriteOptions().ordered(false))
      .toFuture()
      .map(_ => euVatRates)
  }

}
