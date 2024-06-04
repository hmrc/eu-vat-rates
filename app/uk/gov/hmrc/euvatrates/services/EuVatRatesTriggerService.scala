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

import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate}
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatRatesTriggerService @Inject()(
                                          euVatRateService: EuVatRateService,
                                          euVatRateRepository: EuVatRateRepository,
                                          appConfig: AppConfig
                                        )(implicit ec: ExecutionContext) extends Logging {


  def triggerFeedUpdate(monthToSearch: LocalDate): Future[Seq[EuVatRate]] = {

    if (appConfig.schedulerEnabled) {

      val allCountries = Country.euCountriesWithNI

      logger.info(s"Triggered a feed update for country codes: ${allCountries.map(_.code)}")

      getRatesAndSave(allCountries, monthToSearch).map { rates =>
        logger.info(s"EU VAT Rates update ran successfully. Stored ${rates.size} vat rates")
        rates
      }
    } else {
      logger.info("EU VAT rates Scheduler is disabled")
      Seq.empty.toFuture
    }
  }

  private def getRatesAndSave(countries: Seq[Country], monthToSearch: LocalDate): Future[Seq[EuVatRate]] = {

    val dateFrom = monthToSearch.withDayOfMonth(1)
    val dateTo = dateFrom.plusMonths(1).minusDays(1)

    euVatRateService.getAllVatRates(countries, dateFrom, dateTo).map(vatRates => vatRates.sortBy(_.situatedOn.toEpochDay))
      .flatMap { feeds =>
        if (feeds.nonEmpty) {
          euVatRateRepository.setMany(feeds)
        } else {
          logger.warn(s"No rates were retrieved from EC for $countries $dateFrom $dateTo")
          Future.successful(Seq.empty)
        }
      }

  }

}

