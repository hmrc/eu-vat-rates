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

package uk.gov.hmrc.euvatrates.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.http.Status.OK
import play.api.test.Helpers.running
import uk.gov.hmrc.euvatrates.base.{SoapExamples, SpecBase}
import uk.gov.hmrc.http.HeaderCarrier


class ECSoapConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.ec-vat-rates.port" -> server.port)
      .build()
  }

  "ECSoapConnectorSpec" - {

    ".getVatRates" - {

      val wsdlUrl: String = s"/eu-vat-rates-stub/taxation_customs/tedb/ws/VatRetrievalService.wsdl"

      val soapEnvelope = SoapExamples.exampleRequest.get

      "must return OK with a payload of EU VAT Rates" in {

        val app = application

        running(app) {
          val connector = app.injector.instanceOf[ECSoapConnector]

          val responseBody = SoapExamples.exampleResponse.get

          server.stubFor(
            post(urlEqualTo(wsdlUrl))
              .withRequestBody(equalTo(soapEnvelope))
              .willReturn(ok()
                .withBody(responseBody)
              )
          )

          val result = connector.getVatRates(soapEnvelope).futureValue

          result.status mustBe OK
        }
      }
    }
  }

}
