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

import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.models.{EuVatRate, VatRateType}
import uk.gov.hmrc.euvatrates.models.Country.euCountries
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class EuVatRateRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EuVatRate]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val country = euCountries.head
  private val country2 = euCountries.reverse.head
  private val situatedOnDate = LocalDate.of(2023, 1, 1)
  private val fromDate = LocalDate.of(2024, 1, 1)
  private val toDate = LocalDate.of(2025, 1, 31)
  private val fromDate2 = LocalDate.of(2024, 2, 1)
  private val toDate2 = LocalDate.of(2025, 2, 28)
  private val fromDate3 = LocalDate.of(2024, 3, 1)
  private val toDate3 = LocalDate.of(2025, 3, 31)

  private val euVatRate: EuVatRate = EuVatRate(
    country = country,
    vatRate = BigDecimal(5.5),
    vatRateType = VatRateType.Standard,
    situatedOn = situatedOnDate,
    dateFrom = fromDate,
    dateTo = toDate,
    lastUpdated = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  )

  private val euVatRate2: EuVatRate = EuVatRate(
    country = country2,
    vatRate = BigDecimal(10),
    vatRateType = VatRateType.Reduced,
    situatedOn = situatedOnDate,
    dateFrom = fromDate2,
    dateTo = toDate2,
    lastUpdated = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  )

  private val euVatRate3: EuVatRate = EuVatRate(
    country = country2,
    vatRate = BigDecimal(10),
    vatRateType = VatRateType.Reduced,
    situatedOn = situatedOnDate,
    dateFrom = fromDate3,
    dateTo = toDate3,
    lastUpdated = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  )

  private val mockAppConfig = mock[AppConfig]

  protected override val repository = new EuVatRateRepository(
    mongoComponent = mongoComponent,
    mockAppConfig
  )

  ".set" - {

    "must save data" in {
      val setResult = repository.set(euVatRate).futureValue
      val updatedRecord = findAll().futureValue.headOption.value

      setResult mustEqual euVatRate
      updatedRecord mustEqual euVatRate
    }
  }

  ".setMany" - {

    "must save data" in {
      val setResult = repository.setMany(Seq(euVatRate, euVatRate2)).futureValue
      val updatedRecord = findAll().futureValue

      setResult mustEqual Seq(euVatRate, euVatRate2)
      updatedRecord mustEqual Seq(euVatRate, euVatRate2)
    }
  }

  ".get" - {

    "must return saved record when one exists for this user id" in {

      repository.set(euVatRate).futureValue

      val result = repository.get(country, fromDate, toDate).futureValue

      result mustEqual Seq(euVatRate)
    }

  }

  ".getMany" - {

    "must return saved record when one exists for this user id" in {

      val euVatRate4 = euVatRate.copy(country = country2)

      repository.set(euVatRate).futureValue
      repository.set(euVatRate4).futureValue

      val result = repository.getMany(Seq(country, country2), fromDate, toDate).futureValue

      result mustEqual Seq(euVatRate, euVatRate4)
    }

    "must return multiple vat rates over the period" in {

      repository.set(euVatRate).futureValue
      repository.set(euVatRate2).futureValue
      repository.set(euVatRate3).futureValue

      val result = repository.getMany(Seq(country, country2), fromDate, toDate3).futureValue

      result must contain theSameElementsAs Seq(euVatRate, euVatRate2, euVatRate3)
    }


    "doesn't return dates outside of dateFrom/to" in {

      val euVatRate4 = euVatRate.copy(country = country2)

      repository.set(euVatRate).futureValue
      repository.set(euVatRate4).futureValue

      val result = repository.getMany(Seq(country, country2), fromDate.plusYears(11), toDate.plusYears(11)).futureValue

      result mustEqual Seq.empty
    }

  }
}

