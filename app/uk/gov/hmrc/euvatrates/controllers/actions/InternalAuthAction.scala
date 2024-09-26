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

import play.api.mvc._
import uk.gov.hmrc.euvatrates.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.internalauth.client._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait InternalAuthAction extends ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request]

class AuthenticatedIdentifierAction @Inject()(
                                               val parser: BodyParsers.Default,
                                               auth: BackendAuthComponents
                                             )(implicit val executionContext: ExecutionContext) extends InternalAuthAction {

  private val permission = Predicate.Permission(
      resource = Resource.from("eu-vat-rates", resourceLocation = "*"),
      action = IAAction("READ")
    )

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    auth.authorizedAction(
      permission,
      onUnauthorizedError = Results.Unauthorized.toFuture,
      onForbiddenError = Results.Forbidden.toFuture).invokeBlock(request, block)
  }
}
