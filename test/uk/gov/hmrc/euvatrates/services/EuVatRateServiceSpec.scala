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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.euvatrates.base.SpecBase
import uk.gov.hmrc.euvatrates.connectors.ECSoapConnector
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global

class EuVatRateServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockEcVatRateConnector = mock[ECSoapConnector]
  private val mockEuVatRateRepository = mock[EuVatRateRepository]

  override protected def beforeEach(): Unit = {
    Mockito.reset(mockEcVatRateConnector)
    Mockito.reset(mockEuVatRateRepository)
  }

  "EuVatRateService" - {
    "#getAllVatRates" - {

      "should return successfully" - {
        "when connector returns successfully" in {
          val responseXml = <env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/">
            <env:Header/>
            <env:Body>
              <ns0:retrieveVatRatesRespMsg xmlns="urn:ec.europa.eu:taxud:tedb:services:v1:IVatRetrievalService:types"
                                           xmlns:ns0="urn:ec.europa.eu:taxud:tedb:services:v1:IVatRetrievalService">
                <additionalInformation/>
                <vatRateResults>
                  <memberState>{country1.code}</memberState>
                  <type>REDUCED</type>
                  <rate>
                    <type>REDUCED_RATE</type>
                    <value>5.5</value>
                  </rate>
                  <situationOn>2023-05-01+01:00</situationOn>
                </vatRateResults>
                <vatRateResults>
                  <memberState>{country2.code}</memberState>
                  <type>STANDARD</type>
                  <rate>
                    <type>STANDARD</type>
                    <value>20</value>
                  </rate>
                  <situationOn>2023-01-01+01:00</situationOn>
                </vatRateResults>
              </ns0:retrieveVatRatesRespMsg>
            </env:Body>
          </env:Envelope>


          when(mockEcVatRateConnector.getVatRates(any())) thenReturn HttpResponse(200, responseXml.toString()).toFuture
          when(mockEuVatRateRepository.set(any())) thenReturn euVatRate1.toFuture

          val service = new EuVatRateService(mockEuVatRateRepository, mockEcVatRateConnector)

          val result = service.getAllVatRates(countries, dateFrom = dateFrom, dateTo = dateTo)

          result.futureValue must contain theSameElementsAs Seq(euVatRate1, euVatRate2)
          verify(mockEuVatRateRepository, times(1)).set(euVatRate1)
          verify(mockEuVatRateRepository, times(1)).set(euVatRate2)
        }

        "when connector returns unsuccessfully but cache i`s available" in {
          when(mockEcVatRateConnector.getVatRates(any())) thenReturn HttpResponse(500, "Error").toFuture
          when(mockEuVatRateRepository.get(eqTo(country1), any())) thenReturn Seq(euVatRate1).toFuture
          when(mockEuVatRateRepository.get(eqTo(country2), any())) thenReturn Seq(euVatRate2).toFuture

          val service = new EuVatRateService(mockEuVatRateRepository, mockEcVatRateConnector)

          val result = service.getAllVatRates(countries, dateFrom = dateFrom, dateTo = dateTo)

          result.futureValue must contain theSameElementsAs Seq(euVatRate1, euVatRate2)
          verify(mockEuVatRateRepository, times(1)).get(country1, dateFrom)
          verify(mockEuVatRateRepository, times(1)).get(country2, dateFrom)
        }
      }
    }
  }

}
