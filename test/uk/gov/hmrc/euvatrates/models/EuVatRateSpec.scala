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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}

class EuVatRateSpec extends AnyFreeSpec with Matchers {

  val country: Country = Country("AT", "Austria")
  val vatRateType: VatRateType = VatRateType.Standard
  val vatRate: BigDecimal = BigDecimal(20.5)
  val situatedOn: LocalDate = LocalDate.of(2022, 1, 1)
  val dateFrom: LocalDate = LocalDate.of(2022, 1, 1)
  val dateTo: LocalDate = LocalDate.of(2023, 1, 1)
  val lastUpdated: Instant = Instant.now().minus(1, ChronoUnit.DAYS)

  val validEuVatRate: EuVatRate = EuVatRate(
    country,
    vatRate,
    vatRateType,
    situatedOn,
    dateFrom,
    dateTo,
    lastUpdated
  )

  "EuVatRate JSON format" - {

    "serialize and deserialize correctly" in {

      val validEuVatRate = EuVatRate(
        country = Country("AT", "Austria"),
        vatRate = BigDecimal(20.0),
        vatRateType = VatRateType.Standard,
        situatedOn = LocalDate.parse("2022-01-01"),
        dateFrom = LocalDate.parse("2022-01-01"),
        dateTo = LocalDate.parse("2023-01-01"),
        lastUpdated = Instant.now
      )

      val json = Json.toJson(validEuVatRate)

      (json \ "country" \ "code").as[String] mustBe "AT"
      (json \ "vatRate").as[BigDecimal] mustBe validEuVatRate.vatRate
      (json \ "vatRateType").as[String] mustBe "STANDARD"
      (json \ "situatedOn").as[LocalDate] mustBe validEuVatRate.situatedOn
      (json \ "dateFrom").as[LocalDate] mustBe validEuVatRate.dateFrom
      (json \ "dateTo").as[LocalDate] mustBe validEuVatRate.dateTo
      (json \ "lastUpdated").as[Instant] mustBe validEuVatRate.lastUpdated

      val deserialized = json.as[EuVatRate]

      deserialized mustBe validEuVatRate
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj(
        "countryCode" -> "XX",
        "vatRate" -> "invalid",
        "vatRateType" -> "UNKNOWN",
        "situatedOn" -> "invalid-date",
        "dateFrom" -> "invalid-date",
        "dateTo" -> "invalid-date",
        "lastUpdated" -> "invalid-instant"
      )

      val result = invalidJson.asOpt[EuVatRate]
      result mustBe None
    }
  }

  "Country.getCountryFromCode" - {

    "return Some(Country) for valid country code" in {
      val countryOpt = Country.getCountryFromCode("AT")
      countryOpt mustBe Some(country)

      countryOpt.get.code mustBe "AT"
    }

    "return None for invalid country code" in {
      val countryOpt = Country.getCountryFromCode("XX")
      countryOpt mustBe None
    }

    "handle invalid country code gracefully" in {
      val countryOpt = Country.getCountryFromCode("XX")
      countryOpt mustBe None
    }
  }

  "EuVatRate" - {

    "handle invalid vatRate type gracefully" in {
      val invalidVatRateJson = Json.obj(
        "countryCode" -> "AT",
        "vatRate" -> "invalid",
        "vatRateType" -> "STANDARD",
        "situatedOn" -> "2022-01-01",
        "dateFrom" -> "2022-01-01",
        "dateTo" -> "2023-01-01",
        "lastUpdated" -> lastUpdated.toString
      )

      val result = invalidVatRateJson.validate[EuVatRate]
      result mustBe a[JsError]
    }

    "fail when required fields are missing" in {
      val missingFieldsJson = Json.obj(
        "vatRate" -> 20.5,
        "vatRateType" -> "STANDARD",
        "situatedOn" -> "2022-01-01",
        "dateFrom" -> "2022-01-01",
        "dateTo" -> "2023-01-01",
        "lastUpdated" -> lastUpdated.toString
      )

      val result = missingFieldsJson.validate[EuVatRate]
      result mustBe a[JsError]
    }
  }
}

