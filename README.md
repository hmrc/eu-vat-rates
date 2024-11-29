# eu-vat-rates

This API retrieves and stores VAT rate data from the [EC TEDB](https://ec.europa.eu/taxation_customs/tedb/#/home) feed.
On start, the service retrieves the rates and the subsequently daily after that. The initial delay and interval are
configured in the application.conf. The rates are retrieved for the last 3 years and are saved in the database unless
they've already been stored and held for 28 days.


Resources
----------

| Method | URL                                                                       | Description                                                                     | Example response                                                                                                    |
|:------:|---------------------------------------------------------------------------|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
|  GET   | /vat-rates/vat-rate/{countryCode}                                         | Retrieves VAT rates countryCode for the date range of 2021-01-01 to today.      | <pre>{<br>"countryCode":"AT",<br>"vatRate":5.5,<br>"vatRateType":"REDUCED",<br>"situatedOn":"2022-01-01"<br>}</pre> |
|  GET   | /vat-rates/vat-rate/{countryCode}?startDate={startDate}&endDate={endDate} | Retrieves VAT rates countryCode for the date range of {startDate} to {endDate}. | <pre>{<br>"countryCode":"XI",<br>"vatRate":10,<br>"vatRateType":"STANDARD",<br>"situatedOn":"2023-12-10"<br>}</pre> |

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least
a [JRE] to run.

## Run the application locally via Service Manager

```
sm2 --start EU_VAT_RATES_ALL
```

### To run the application locally from the repository, execute the following:

```
sm2 --stop EU_VAT_RATES
```

and

```
sbt run
```

## Authentication

This service use [internal-auth](https://github.com/hmrc/internal-auth) to authenticate requests using the service-to-service auth pattern.

To allow your service to access eu-vat-rates, you will need to add your service name to the ```grants.yaml``` in [internal-auth-config](https://github.com/hmrc/internal-auth-config). There are separate files for this in QA and Production. It will look similar to below:

```yaml
- grantees:
    service: [ ioss-returns-frontend, one-stop-shop-returns-frontend ]
  permissions:
    - resourceType: eu-vat-rates
      resourceLocation: '*'
      actions: [ READ ]
```

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session on the browser.

### Unit Tests

To run all tests, run the following command in your sbt session:

```
test
```

To run a single test, run the following command in your sbt session:

```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:

```
testOnly *EuVatRateControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:

```
it / test
```

To run a single test, run the following command in your sbt session:

```
it / testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:

```
it / testOnly *EuVatRateRepositorySpec
```

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").