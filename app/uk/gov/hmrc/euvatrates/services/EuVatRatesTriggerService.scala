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

import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate}
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatRatesTriggerService @Inject()(
                                              euVatRateService: EuVatRateService,
                                              euVatRateRepository: EuVatRateRepository,
                                              clock: Clock
                                   ) (implicit ec: ExecutionContext) extends Logging {



  def triggerFeedUpdate: Future[Seq[EuVatRate]] = {
    val allCountries = Country.euCountriesWithNI

    getRatesAndSave(allCountries)
  }

  private def getRatesAndSave(countries: Seq[Country]): Future[Seq[EuVatRate]] = {
    val defaultStartDate = LocalDate.of(2021, 1, 1)
    val defaultEndDate = LocalDate.now(clock)

    euVatRateService.getAllVatRates(countries, defaultStartDate, defaultEndDate).map(vatRates => vatRates.sortBy(_.situatedOn.toEpochDay))
      .flatMap(feeds =>
        if (feeds.nonEmpty) {
          euVatRateRepository.setMany(feeds)
        } else {
          logger.warn("No rates were retrieved from EC")
          Future.successful(Seq.empty)
        }
      )
  }

}

