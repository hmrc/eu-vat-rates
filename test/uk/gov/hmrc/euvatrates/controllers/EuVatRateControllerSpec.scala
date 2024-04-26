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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.euvatrates.base.SpecBase
import uk.gov.hmrc.euvatrates.services.EuVatRateService
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

class EuVatRateControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val countryCode = "AT"
  private lazy val fakeRequest = FakeRequest(GET, routes.EuVatRateController.getVatRateForCountry(countryCode).url)
  private val mockEuVatRateService = mock[EuVatRateService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEuVatRateService)
  }

  "GET /" - {
    "return 200" in {

      when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq.empty.toFuture

      val app =
        applicationBuilder
          .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
          .build()

      running(app) {
        val result = route(app, fakeRequest).value
        status(result) mustEqual Status.OK
      }
    }
  }
}
