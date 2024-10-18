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

import play.api.Configuration
import play.api.http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.euvatrates.config.Service
import uk.gov.hmrc.euvatrates.logging.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ECSoapConnector @Inject()(
                                 config: Configuration,
                                 httpClient: HttpClientV2,
                               )(implicit ec: ExecutionContext) extends Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.ec-vat-rates")

  def getVatRates(soapEnvelope: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val url: URL = url"$baseUrl/taxation_customs/tedb/ws/VatRetrievalService.wsdl"

    val headers = Seq(
      "SOAPAction" -> "urn:ec.europa.eu:taxud:tedb:services:v1:VatRetrievalService/RetrieveVatRates",
      CONTENT_TYPE -> "text/xml"
    )

    logger.debug(s"Posting with url $url and env $soapEnvelope with headers $headers")

    httpClient
      .post(url)
      .setHeader(headers: _*)
      .withBody(soapEnvelope)
      .withProxy
      .execute[HttpResponse]
  }

}
