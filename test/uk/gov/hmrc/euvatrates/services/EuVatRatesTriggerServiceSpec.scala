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

package uk.gov.hmrc.euvatrates.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.euvatrates.base.SpecBase
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.models.Country
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class EuVatRatesTriggerServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockEuVatRateService = mock[EuVatRateService]
  private val mockEuVatRateRepository = mock[EuVatRateRepository]
  private val mockAppConfig = mock[AppConfig]

  private val instant: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockEuVatRateService,
      mockEuVatRateRepository,
      mockAppConfig
    )
  }

  "EUVatRatesTriggerService" - {

    "#triggerFeedUpdate" - {
      "return rates when scheduler is enabled" in {

        when(mockAppConfig.schedulerEnabled) thenReturn true
        when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture
        when(mockEuVatRateRepository.setMany(any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture

        val service = new EuVatRatesTriggerService(mockEuVatRateService, mockEuVatRateRepository, mockAppConfig, stubClock)

        val allExpectedDatesToSearch = {
          val now = LocalDate.now(stubClockAtArbitraryDate)

          val defaultStartDate = now.minusYears(3).minusMonths(1)
          val defaultEndDate = now

          allMonthsBetweenDates(defaultStartDate, defaultEndDate)
        }

        val allExpectedVatRates = allExpectedDatesToSearch.flatMap { _ =>

          Seq(
            euVatRate1,
            euVatRate2
          )
        }

        val result = service.triggerFeedUpdate.futureValue

        result.size mustBe allExpectedDatesToSearch.size * 2

        service.triggerFeedUpdate.futureValue must contain theSameElementsAs allExpectedVatRates
      }

      "return empty when scheduler is disabled" in {

        when(mockAppConfig.schedulerEnabled) thenReturn false

        val service = new EuVatRatesTriggerService(mockEuVatRateService, mockEuVatRateRepository, mockAppConfig, stubClock)

        val result = service.triggerFeedUpdate.futureValue

        result.size mustBe 0
      }
    }

  }

  private def allMonthsBetweenDates(currentMonth: LocalDate, endDate: LocalDate): List[LocalDate] = {
    if (currentMonth.withDayOfMonth(1).isEqual(endDate.withDayOfMonth(1))) {
      List(currentMonth)
    } else {
      List(currentMonth) ++ allMonthsBetweenDates(currentMonth.plusMonths(1), endDate)
    }
  }

}

