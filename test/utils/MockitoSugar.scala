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

package utils

import org.mockito.ArgumentMatchers.argThat
import org.mockito.{ArgumentMatcher, Mockito}
import org.mockito.Mockito.RETURNS_DEEP_STUBS

import scala.reflect.ClassTag
import scala.reflect.*

object MockitoSugar {
  def mock[T: ClassTag]: T = Mockito.mock(classTag[T].runtimeClass.asInstanceOf[Class[T]])
  def deepMock[T: ClassTag]: T = Mockito.mock(classTag[T].runtimeClass.asInstanceOf[Class[T]], RETURNS_DEEP_STUBS)

  def varArgsEq[T](required: T*): T = argThat(new ArgumentMatcher[Any] {
    def matches(v: Any): Boolean = required == v.asInstanceOf[Seq[T]]
    override def toString: String = s"[" + required.mkString(",") + "]"
  }.asInstanceOf[ArgumentMatcher[T]])

}
