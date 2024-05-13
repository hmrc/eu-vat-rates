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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.Country
import uk.gov.hmrc.euvatrates.services.EuVatRateService
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class EuVatRateController @Inject()(
                                     cc: ControllerComponents,
                                     euVatRateService: EuVatRateService,
                                     clock: Clock
                                   )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getVatRateForCountry(country: String, startDate: Option[String], endDate: Option[String]): Action[AnyContent] = Action.async { implicit request =>

    logger.info(s"Received request with start date $startDate and end date $endDate")

    val defaultStartDate = LocalDate.of(2021, 1, 1)
    val defaultEndDate = LocalDate.now(clock)

    val dateFrom = startDate.map(LocalDate.parse).getOrElse(defaultStartDate)
    val dateTo = endDate.map(LocalDate.parse).getOrElse(defaultEndDate)

    if(dateFrom.isAfter(dateTo)) {
      BadRequest("Date from cannot be after date to").toFuture
    } else {

      euVatRateService.getAllVatRates(
        countries = Seq(Country.getCountryFromCode(country).get),
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
}
