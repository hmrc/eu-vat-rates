package uk.gov.hmrc.euvatrates.repositories

import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.euvatrates.config.AppConfig
import uk.gov.hmrc.euvatrates.models.{Country, EuVatRate, VatRateType}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class EuVatRatesRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EuVatRate]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val fromDate = LocalDate.of(2024, 1, 1)

  private val euVatRate: EuVatRate = EuVatRate(
    country = Country("ES", "Spain"),
    vatRate = BigDecimal(5.5),
    vatRateType = VatRateType.Standard,
    validFrom = fromDate,
    validTo = LocalDate.of(2024, 3, 1)
  )

  private val mockAppConfig = mock[AppConfig]

  protected override val repository = new EuVatRateRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must save data" in {
      val setResult = repository.set(euVatRate).futureValue
      val updatedRecord = findAll().futureValue.headOption.value

      setResult mustEqual euVatRate
      updatedRecord mustEqual euVatRate
    }
  }

  ".get" - {

    "must return saved record when one exists for this user id" in {

      repository.set(euVatRate).futureValue

      val result = repository.get(country, fromDate).futureValue

      result.value mustEqual euVatRate
    }

    "must return None when no data exists" in {

      val result = repository.get("").futureValue

      result must not be defined
    }
  }
}

