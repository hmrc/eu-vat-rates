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
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate}
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class EuVatRatesTriggerServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockEuVatRateService = mock[EuVatRateService]
  private val mockEuVatRateRepository = mock[EuVatRateRepository]

  private val instant: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

  override def beforeEach(): Unit = {
    Mockito.reset(mockEuVatRateService)
    Mockito.reset(mockEuVatRateRepository)
  }

  "EUVatRatesTriggerService" - {

    "#triggerFeedUpdate" in {

      when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture
      when(mockEuVatRateRepository.setMany(any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture

      val service = new EuVatRatesTriggerService(mockEuVatRateService, mockEuVatRateRepository, stubClock)

      service.triggerFeedUpdate.futureValue mustBe Seq(euVatRate1, euVatRate2)
    }

  }

}

