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

package uk.gov.hmrc.euvatrates.models

import play.api.libs.json.{Json, OFormat}

case class Country(code: String, name: String)

object Country {

  val euCountries: Seq[Country] = Seq(
    Country("AT", "Austria"),
    Country("BE", "Belgium"),
    Country("BG", "Bulgaria"),
    Country("HR", "Croatia"),
    Country("CY", "Republic of Cyprus"),
    Country("CZ", "Czech Republic"),
    Country("DK", "Denmark"),
    Country("EE", "Estonia"),
    Country("FI", "Finland"),
    Country("FR", "France"),
    Country("DE", "Germany"),
    Country("EL", "Greece"),
    Country("HU", "Hungary"),
    Country("IE", "Ireland"),
    Country("IT", "Italy"),
    Country("LV", "Latvia"),
    Country("LT", "Lithuania"),
    Country("LU", "Luxembourg"),
    Country("MT", "Malta"),
    Country("NL", "Netherlands"),
    Country("PL", "Poland"),
    Country("PT", "Portugal"),
    Country("RO", "Romania"),
    Country("SK", "Slovakia"),
    Country("SI", "Slovenia"),
    Country("ES", "Spain"),
    Country("SE", "Sweden")
  )

  val northernIreland: Country =
    Country("XI", "Northern Ireland")

  val euCountriesWithNI: Seq[Country] = euCountries :+ northernIreland

  def getCountryFromCode(countryCode: String): Option[Country] = euCountriesWithNI.find(_.code == countryCode)

  implicit val format: OFormat[Country] = Json.format[Country]

}