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

package uk.gov.hmrc.euvatrates.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.euvatrates.controllers.actions.InternalAuthAction
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.Country
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.services.EuVatRateService
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton()
class EuVatRateController @Inject()(
                                     cc: ControllerComponents,
                                     euVatRateService: EuVatRateService,
                                     euVatRateRepository: EuVatRateRepository,
                                     clock: Clock,
                                     auth: InternalAuthAction
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getVatRateForCountry(country: String, startDate: Option[String], endDate: Option[String]): Action[AnyContent] = auth.compose(Action).async {

    logger.info(s"Received request with start date $startDate and end date $endDate")

    val defaultStartDate = LocalDate.of(2021, 1, 1)
    val defaultEndDate = LocalDate.now(clock)

    val maybeDateFrom = Try(startDate.map(LocalDate.parse).getOrElse(defaultStartDate))
    val maybeDateTo = Try(endDate.map(LocalDate.parse).getOrElse(defaultEndDate))

    val countriesPassed = Country.getCountryFromCode(country).toSeq

    maybeDateFrom match {
      case Success(dateFrom) => {
        maybeDateTo match {
          case Success(dateTo) =>

            if (countriesPassed.isEmpty) {
              BadRequest(s"A valid country was not provided [$country]").toFuture
            } else {

              if (dateFrom.isAfter(dateTo)) {
                BadRequest("Date from cannot be after date to").toFuture
              } else {

                euVatRateRepository.getMany(
                  countries = countriesPassed,
                  fromDate = dateFrom,
                  toDate = dateTo
                ).flatMap {
                  case Nil =>
                    logger.warn(s"Did not find any rates in store for the following countries: $countriesPassed with dates from $dateFrom and to $dateTo")
                    fallbackCall(countriesPassed, dateFrom, dateTo)
                  case rates =>
                    Ok(Json.toJson(rates)).toFuture
                }.recoverWith {
                  case e =>
                    logger.warn(s"Unable to get the following countries from store $countriesPassed with dates from $dateFrom and to $dateTo because of ${e.getMessage}", e)
                    fallbackCall(countriesPassed, dateFrom, dateTo)
                }
              }
            }
          case Failure(e) =>
            BadRequest(s"dateTo was not processable ${e.getMessage}").toFuture
        }
      }
      case Failure(e) =>
        BadRequest(s"dateFrom was not processable ${e.getMessage}").toFuture
    }
  }

  private def fallbackCall(countriesPassed: Seq[Country], dateFrom: LocalDate, dateTo: LocalDate): Future[Result] = {

    euVatRateService.getAllVatRates(
      countries = countriesPassed,
      dateFrom = dateFrom,
      dateTo = dateTo
    ).map(resp =>
      Ok(Json.toJson(resp))
    ).recover {
      case e: Exception =>
        logger.error(s"Error occurred while getting VAT rates ${e.getMessage}", e)
        InternalServerError(e.getMessage)
    }
  }
}
