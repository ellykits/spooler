/*
* Copyright 2026 Spooler Contributors
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
package io.spooler.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RenderLabelTest {
  private val label =
    Label(
      widthDots = 609,
      heightDots = 406,
      elements = listOf(LabelElement.Text(xDots = 10, yDots = 20, text = "ITEM-1")),
    )

  @Test
  fun dispatchesZplToZplRenderer() {
    assertEquals(ZplRenderer.render(label), renderLabel(label, LabelDialect.ZPL))
  }

  @Test
  fun dispatchesTsplToTsplRenderer() {
    assertEquals(TsplRenderer.render(label), renderLabel(label, LabelDialect.TSPL))
  }
}
