/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.euvatrates.scheduler

import org.apache.http.concurrent.Cancellable
import org.apache.pekko.actor.ActorSystem
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.euvatrates.services.EuVatRatesTriggerService
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

@Singleton
class EuVatRatesWorker @Inject()(
                                  configuration: Configuration,
                                  euVatRatesTriggerService: EuVatRatesTriggerService,
                                  lifecycle: ApplicationLifecycle,
                                  actorSystem: ActorSystem,
                                  clock: Clock
                                )(implicit ec: ExecutionContext) extends Logging {

  private val scheduler = actorSystem.scheduler

  private val interval = configuration.get[FiniteDuration]("schedules.eu-vat-rates-worker.interval")
  private val initialDelay = configuration.get[FiniteDuration]("schedules.eu-vat-rates-worker.initialDelay")

  private val cancel = {
    val now = LocalDate.now(clock)

    val defaultStartDate = now.minusYears(3).minusMonths(1)
    val defaultEndDate = now

    val allMonthsToSearch = allMonthsBetweenDates(defaultStartDate, defaultEndDate)

    allMonthsToSearch.zipWithIndex.map { case (date, index) =>
      scheduler.scheduleWithFixedDelay(
        initialDelay = initialDelay * index,
        delay = interval
      ) { () =>
        euVatRatesTriggerService.triggerFeedUpdate(date).recover {
          case e => logger.error("Error when updating EU Vat rates", e)
        }
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

  lifecycle.addStopHook { () =>

    Future.sequence(
      cancel.map(cancellable => cancellable.cancel().toFuture)
    )
  }
}

