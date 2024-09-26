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
import uk.gov.hmrc.euvatrates.controllers.auth.InternalAuthAction

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeInternalAuthAction @Inject()(bodyParsers: PlayBodyParsers) extends InternalAuthAction {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    println("FakeInternalAuthAction invoked, bypassing authentication")
    block(request)
  }

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def parser: BodyParser[AnyContent] = bodyParsers.default

}
