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

package uk.gov.hmrc.euvatrates.controllers.actions

import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.http.Status
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.euvatrates.base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InternalAuthActionsSpec extends SpecBase with MockitoSugar with Matchers {

  class Harness(authAction: InternalAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok}
  }

  "Auth Action" - {

    "when the client has not set an auth header is unauthorised" - {

      "must return unauthorized" in {

        val app = applicationBuilder().build()

        running(app) {
          val bodyParsers = app.injector.instanceOf[BodyParsers.Default]
          val authAction = new AuthenticatedIdentifierAction(bodyParsers, app.injector.instanceOf[BackendAuthComponents])
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "when the client has set an auth header is authorized" - {

      "must return OK" in {

        implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
        val mockStubBehaviour = mock[StubBehaviour]
        val stubAuth = BackendAuthComponentsStub(mockStubBehaviour)

        val app = applicationBuilder()
          .bindings(
            bind[BackendAuthComponents].toInstance(stubAuth)
          ).build()

        running(app) {
          val authAction = app.injector.instanceOf[AuthenticatedIdentifierAction]
          val canAccessPredicate = Predicate.Permission(
            Resource(
              ResourceType("eu-vat-rates"),
              ResourceLocation("*")
            ),
            IAAction("READ")
          )

          when(mockStubBehaviour.stubAuth(eqTo(Some(canAccessPredicate)),
            eqTo(Retrieval.EmptyRetrieval))) thenReturn Future.unit

          val result = authAction.invokeBlock(
            FakeRequest().withHeaders("Authorization" -> "Anything"),
            (_: Request[AnyContent]) => {
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe OK
        }
      }
    }

    "when the client has set a valid auth header is but is unauthorized" - {

      "must return unauthorised" in {

        implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
        val mockStubBehaviour = mock[StubBehaviour]
        val stubAuth = BackendAuthComponentsStub(mockStubBehaviour)

        val app = applicationBuilder()
          .bindings(
            bind[BackendAuthComponents].toInstance(stubAuth)
          ).build()

        running(app) {
          val authAction = app.injector.instanceOf[AuthenticatedIdentifierAction]
          val canAccessPredicate = Predicate.Permission(
            Resource(
              ResourceType("eu-vat-rates"),
              ResourceLocation("*")
            ),
            IAAction("READ")
          )

          when(mockStubBehaviour.stubAuth(eqTo(Some(canAccessPredicate)),
            eqTo(Retrieval.EmptyRetrieval))) thenReturn Future.failed(UpstreamErrorResponse("Unauthorized", Status.UNAUTHORIZED))

          val result = authAction.invokeBlock(
            FakeRequest().withHeaders("Authorization" -> "Anything"),
            (_: Request[AnyContent]) => {
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe UNAUTHORIZED
        }

      }
    }

    "when the client has set a valid auth header but does not have the necessary permission" - {

      "must return forbidden" in {

        implicit val cc: ControllerComponents = Helpers.stubControllerComponents()
        val mockStubBehaviour = mock[StubBehaviour]
        val stubAuth = BackendAuthComponentsStub(mockStubBehaviour)

        val app = applicationBuilder()
          .bindings(
            bind[BackendAuthComponents].toInstance(stubAuth)
          ).build()

        running(app) {
          val authAction = app.injector.instanceOf[AuthenticatedIdentifierAction]
          val canAccessPredicate = Predicate.Permission(
            Resource(
              ResourceType("eu-vat-rates"),
              ResourceLocation("*")
            ),
            IAAction("READ")
          )

          when(mockStubBehaviour.stubAuth(eqTo(Some(canAccessPredicate)),
            eqTo(Retrieval.EmptyRetrieval))) thenReturn Future.failed(UpstreamErrorResponse("Forbidden", Status.FORBIDDEN))

          val result = authAction.invokeBlock(
            FakeRequest().withHeaders("Authorization" -> "Anything"),
            (_: Request[AnyContent]) => {
              Future.successful(Results.Ok)
            }
          )

          status(result) mustBe FORBIDDEN
        }
      }
    }
  }

}
