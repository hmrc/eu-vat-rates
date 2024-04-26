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

import java.time.LocalDate
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
  private val fromDate = LocalDate.of(2024, 1, 1)

  private val euVatRate: EuVatRate = EuVatRate(
    country = country,
    vatRate = BigDecimal(5.5),
    vatRateType = VatRateType.Standard,
    situatedOn = fromDate
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

  ".get" - {

    "must return saved record when one exists for this user id" in {

      repository.set(euVatRate).futureValue

      val result = repository.get(country, fromDate).futureValue

      result mustEqual Seq(euVatRate)
    }

  }
}

