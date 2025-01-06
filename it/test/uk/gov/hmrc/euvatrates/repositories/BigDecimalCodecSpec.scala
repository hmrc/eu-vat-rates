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

package uk.gov.hmrc.euvatrates.repositories


import org.bson.{BsonInvalidOperationException, BsonReader, BsonWriter}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doThrow, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock


class BigDecimalCodecSpec extends AnyFreeSpec with Matchers {

  "BigDecimalCodec" - {

    "should encode a BigDecimal to Decimal128" in {
      val writer = mock[BsonWriter]
      val bigDecimalValue = BigDecimal(5.5)

      BigDecimalCodec.encode(writer, bigDecimalValue, null)

      verify(writer).writeDecimal128(any())
    }

    "should decode Decimal128 to BigDecimal" in {

      val reader = mock[BsonReader]
      val decimal128 = new org.bson.types.Decimal128(new java.math.BigDecimal("5.5"))
      when(reader.readDecimal128()).thenReturn(decimal128)

      val decodedValue = BigDecimalCodec.decode(reader, null)

      decodedValue mustEqual BigDecimal(5.5)
    }

    "should throw an exception if the value is not a valid Decimal128 during decoding" in {

      val reader = mock[BsonReader]
      when(reader.readDecimal128()).thenThrow(new BsonInvalidOperationException("Invalid BSON data"))

      assertThrows[BsonInvalidOperationException] {
        BigDecimalCodec.decode(reader, null)
      }
    }

    "should throw an exception if writing invalid Decimal128" in {

      val writer = mock[BsonWriter]
      val bigDecimalValue = BigDecimal(5.5)

      doThrow(new RuntimeException("Write failed")).when(writer).writeDecimal128(any())

      assertThrows[RuntimeException] {
        BigDecimalCodec.encode(writer, bigDecimalValue, null)
      }
    }
  }
}

