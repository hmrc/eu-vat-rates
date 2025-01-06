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

package uk.gov.hmrc.euvatrates.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate}

case class EuVatRate(
                      country: Country,
                      vatRate: BigDecimal,
                      vatRateType: VatRateType,
                      situatedOn: LocalDate,
                      dateFrom: LocalDate,
                      dateTo: LocalDate,
                      lastUpdated: Instant
                    )

object EuVatRate {
  private val dbReads: Reads[EuVatRate] = {
    (
      (__ \ "countryCode").read[String].map(code => Country.getCountryFromCode(code).get) and
        (__ \ "vatRate").read[BigDecimal] and
        (__ \ "vatRateType").read[VatRateType] and
        (__ \ "situatedOn").read(MongoJavatimeFormats.localDateFormat) and
        (__ \ "dateFrom").read(MongoJavatimeFormats.localDateFormat) and
        (__ \ "dateTo").read(MongoJavatimeFormats.localDateFormat) and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      ) (EuVatRate.apply _)
  }
  private val dbWrites: OWrites[EuVatRate] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "countryCode").write[String].contramap[Country](_.code) and
        (__ \ "vatRate").write[BigDecimal] and
        (__ \ "vatRateType").write[VatRateType] and
        (__ \ "situatedOn").write(MongoJavatimeFormats.localDateFormat) and
        (__ \ "dateFrom").write(MongoJavatimeFormats.localDateFormat) and
        (__ \ "dateTo").write(MongoJavatimeFormats.localDateFormat) and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      ) (euVatRate => Tuple.fromProductTyped(euVatRate))
  }

  val dbFormat: Format[EuVatRate] = Format(dbReads, dbWrites)

  implicit val format: Format[EuVatRate] = Json.format[EuVatRate]
}
