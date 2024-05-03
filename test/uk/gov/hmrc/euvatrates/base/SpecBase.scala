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

package uk.gov.hmrc.euvatrates.base

import org.scalatest.{OptionValues, TryValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate, VatRateType}
import uk.gov.hmrc.euvatrates.models.Country.euCountries

import java.time.LocalDate

trait SpecBase extends AnyFreeSpec
  with Matchers
  with TryValues
  with OptionValues
  with ScalaFutures
  with IntegrationPatience
  with MockitoSugar {

  protected def applicationBuilder(): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  val country1: Country = euCountries.head
  val country2: Country = euCountries.reverse.head
  val countries: Seq[Country] = Seq(country1, country2)
  val dateFrom: LocalDate = LocalDate.of(2023, 1, 1)
  val dateTo: LocalDate = LocalDate.of(2024, 1, 1)

  val euVatRate1: EuVatRate = EuVatRate(country1, BigDecimal(5.5), VatRateType.Reduced, LocalDate.of(2023, 5, 1))
  val euVatRate2: EuVatRate = EuVatRate(country2, BigDecimal(20), VatRateType.Standard, LocalDate.of(2023, 1, 1))
}
