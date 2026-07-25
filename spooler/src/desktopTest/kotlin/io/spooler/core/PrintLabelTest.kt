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

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PrintLabelTest {
  private val label =
    Label(
      widthDots = 609,
      heightDots = 406,
      elements = listOf(LabelElement.Text(xDots = 10, yDots = 20, text = "ITEM-1")),
    )

  private val outputs = mutableListOf<File>()

  @AfterTest
  fun cleanup() {
    outputs.forEach { it.delete() }
  }

  private fun tempPath(): String {
    val file = File.createTempFile("spooler-label-test-", ".zpl")
    outputs += file
    return file.absolutePath
  }

  @Test
  fun savesTheZplRenderingToFile() = runBlocking {
    val path = tempPath()

    val result = PrintEngine().printLabel(label, PrintTarget.SaveToFile(path))

    assertTrue(result is PrintResult.Saved, "expected Saved but was $result")
    assertContentEquals(
      renderLabel(label, LabelDialect.ZPL).encodeToByteArray(),
      File(path).readBytes(),
    )
  }

  @Test
  fun failsWhenTheDriverIsNotALabelDriver() = runBlocking {
    val result =
      PrintEngine().printLabel(label, PrintTarget.SendToPrinter(NetworkEscPosDriver("x")))

    assertTrue(result is PrintResult.Failure, "expected Failure but was $result")
  }
}
