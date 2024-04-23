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

import play.api.http.Status.OK
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.connectors.ECSoapConnector
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate, VatRateType}
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, XML}

class EuVatRateService @Inject()(
                                  euVatRateRepository: EuVatRateRepository,
                                  eCSoapConnector: ECSoapConnector
                                )(implicit ec: ExecutionContext) extends Logging {


  def getAllVatRates(countries: Seq[Country], dateFrom: LocalDate, dateTo: LocalDate): Future[Seq[EuVatRate]] = {

    val countryXml = countries.map { country =>
      <urn1:isoCode>{country.code}</urn1:isoCode>
    }

    val messageBody =
      <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:ec.europa.eu:taxud:tedb:services:v1:IVatRetrievalService" xmlns:urn1="urn:ec.europa.eu:taxud:tedb:services:v1:IVatRetrievalService:types">
        <soapenv:Header/>
        <soapenv:Body>
          <urn:retrieveVatRatesReqMsg>
            <urn1:memberStates>
              {countryXml}
            </urn1:memberStates>
            <urn1:from>{dateFrom.toString}</urn1:from>
            <urn1:to>{dateTo}</urn1:to>
          </urn:retrieveVatRatesReqMsg>
        </soapenv:Body>
      </soapenv:Envelope>

    eCSoapConnector.getVatRates(messageBody.toString()).flatMap { response =>
      response.status match {
        case OK =>
          val parsedResponse = parseResponse(response.body)
          cacheResponse(parsedResponse)
          parsedResponse.toFuture
        case status =>
          logger.error(s"Error happened when connecting to EC with status $status and body ${response.body}, retrieving latest cache")
          retrieveFromCache(countries, dateFrom, dateTo)
      }
    }
  }

  private def cacheResponse(vatRates: Seq[EuVatRate]) = {
    Future.sequence(vatRates.map { vatRate =>
      euVatRateRepository.set(vatRate)
    })
  }

  private def retrieveFromCache(countries: Seq[Country], dateFrom: LocalDate, dateTo: LocalDate) = {
    Future.sequence(countries.map { country =>
      euVatRateRepository.get(country, dateFrom)
    }).map(_.flatten)
  }

  private def parseResponse(response: String): Seq[EuVatRate] = {
    val xml = XML.loadString(response)

    (xml \\ "Envelope" \\ "Body" \\ "retrieveVatRatesRespMsg" \\ "vatRateResults").map { vatRateResult =>
      parseSingleEuVatRate(vatRateResult)
    }.groupBy(_.country).flatMap {
      case (_, vatRates) => vatRates.distinctBy(_.vatRate)
    }.toSeq
  }

  private def parseSingleEuVatRate(elem: Node): EuVatRate = {
    val memberStateElem = elem \ "memberState"
    val rateElem = elem \ "rate" \ "value"
    val vatRateTypeElem = elem \ "type"
    val situatedOnElem = elem \ "situationOn"

    EuVatRate(
      country = Country.getCountryFromCode(memberStateElem.text).getOrElse(throw new Exception(s"Unknown Country code ${memberStateElem.text}")),
      vatRate = BigDecimal(rateElem.text),
      vatRateType = VatRateType.enumerable.withName(vatRateTypeElem.text).getOrElse(throw new Exception(s"Unknown vat rate type ${vatRateTypeElem.text}")),
      situatedOn = LocalDate.parse(situatedOnElem.text.split("\\+").head)
    )
  }

}
