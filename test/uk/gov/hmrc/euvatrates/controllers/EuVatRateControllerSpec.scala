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
import uk.gov.hmrc.euvatrates.controllers.actions.FakeInternalAuthAction
import uk.gov.hmrc.euvatrates.controllers.actions.InternalAuthAction
import uk.gov.hmrc.euvatrates.repositories.EuVatRateRepository
import uk.gov.hmrc.euvatrates.services.EuVatRateService
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.Future

class EuVatRateControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val countryCode = "AT"
  private lazy val fakeRequest = FakeRequest(GET, routes.EuVatRateController.getVatRateForCountry(countryCode).url)
  private val mockEuVatRateService = mock[EuVatRateService]
  private val mockEuVatRateRepository = mock[EuVatRateRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockEuVatRateService,
      mockEuVatRateRepository
    )
  }

  "GET /" - {
    "return 200" - {
      "when cache responds successfully" - {
        "with empty data it does the fallback call" in {

          when(mockEuVatRateRepository.getMany(any(), any(), any())) thenReturn Seq.empty.toFuture
          when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq.empty.toFuture

          val app =
            applicationBuilder()
              .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
              .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
              .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
              .build()

          running(app) {
            val result = route(app, fakeRequest).value
            status(result) mustEqual Status.OK
          }
        }

        "when endpoint returns successfully with some data" in {

          when(mockEuVatRateRepository.getMany(any(), any(), any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture

          val app =
            applicationBuilder()
              .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
              .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
              .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
              .build()

          running(app) {
            val result = route(app, fakeRequest).value
            status(result) mustEqual Status.OK
          }
        }
      }

      "when cache responds with an error" - {
        "when endpoint returns successfully with empty data" in {

          when(mockEuVatRateRepository.getMany(any(), any(), any())) thenReturn Future.failed(new Exception("Error occurred"))
          when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq.empty.toFuture

          val app =
            applicationBuilder()
              .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
              .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
              .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
              .build()

          running(app) {
            val result = route(app, fakeRequest).value
            status(result) mustEqual Status.OK
          }
        }

        "when endpoint returns successfully with some data" in {

          when(mockEuVatRateRepository.getMany(any(), any(), any())) thenReturn Future.failed(new Exception("Error occurred"))
          when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq(euVatRate1, euVatRate2).toFuture

          val app =
            applicationBuilder()
              .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
              .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
              .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
              .build()

          running(app) {
            val result = route(app, fakeRequest).value
            status(result) mustEqual Status.OK
          }
        }
      }
    }

    "return 400" - {

      "when date from is after date to" in {

        val app =
          applicationBuilder()
            .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
            .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
            .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
            .build()

        val dateFrom = LocalDate.of(2024, 2, 1).toString
        val dateTo = LocalDate.of(2024, 1, 1).toString

        lazy val fakeRequest = FakeRequest(
          GET,
          routes.EuVatRateController.getVatRateForCountry(
            countryCode,
            startDate = Some(dateFrom),
            endDate = Some(dateTo)
          ).url)

        running(app) {
          val result = route(app, fakeRequest).value
          status(result) mustEqual Status.BAD_REQUEST
        }
      }

      "when the country isn't a valid country" in {

        when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq.empty.toFuture

        lazy val fakeRequest = FakeRequest(
          GET,
          routes.EuVatRateController.getVatRateForCountry(
            "XY"
          ).url)

        val app =
          applicationBuilder()
            .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
            .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
            .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
            .build()

        running(app) {
          val result = route(app, fakeRequest).value
          status(result) mustEqual Status.BAD_REQUEST
        }
      }

      "when the dates aren't valid" in {

        when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Seq.empty.toFuture

        lazy val fakeRequest = FakeRequest(
          GET,
          routes.EuVatRateController.getVatRateForCountry(
            countryCode,
            Some("notADate")
          ).url)

        val app =
          applicationBuilder()
            .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
            .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
            .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
            .build()

        running(app) {
          val result = route(app, fakeRequest).value
          status(result) mustEqual Status.BAD_REQUEST
        }
      }

      "when dateTo is not processable" in {

        val invalidDateTo = "invalid-date"

        lazy val fakeRequest = FakeRequest(
          GET,
          routes.EuVatRateController.getVatRateForCountry(
            countryCode,
            startDate = Some("2024-01-01"), // valid date from
            endDate = Some(invalidDateTo) // invalid date to
          ).url)

        val app =
          applicationBuilder()
            .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
            .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
            .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
            .build()

        running(app) {
          val result = route(app, fakeRequest).value
          status(result) mustEqual Status.BAD_REQUEST
          contentAsString(result) must include("dateTo was not processable") // Ensure the message is in the response
        }
      }

    }

    "return 500" - {

      "when database and endpoint returns with an error" in {

        when(mockEuVatRateRepository.getMany(any(), any(), any())) thenReturn Future.failed(new Exception("Error occurred"))
        when(mockEuVatRateService.getAllVatRates(any(), any(), any())) thenReturn Future.failed(new Exception("error"))

        val app =
          applicationBuilder()
            .overrides(bind[EuVatRateService].toInstance(mockEuVatRateService))
            .overrides(bind[EuVatRateRepository].toInstance(mockEuVatRateRepository))
            .overrides(bind[InternalAuthAction].to(classOf[FakeInternalAuthAction]))
            .build()

        running(app) {
          val result = route(app, fakeRequest).value
          status(result) mustEqual Status.INTERNAL_SERVER_ERROR
        }
      }

    }
  }
}
