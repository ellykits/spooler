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

internal object EscPosBytes {
  const val ESC = 0x1B.toByte()
  const val GS = 0x1D.toByte()
  const val LF = 0x0A.toByte()
}

private fun toPrinterAscii(text: String): String =
  text.map { if (it.code in 32..126) it else '?' }.joinToString("")

private fun wrapLine(line: String, charactersPerLine: Int): List<String> {
  if (charactersPerLine < 1 || line.length <= charactersPerLine) return listOf(line)
  return line.chunked(charactersPerLine)
}

private fun charactersFor(driver: EscPosDriver): Int =
  driver.charactersPerLine ?: if (driver.paperWidthMm <= 58) 32 else 48

internal fun buildEscPos(text: String, driver: EscPosDriver): ByteArray {
  val out = ArrayList<Byte>()
  out.add(EscPosBytes.ESC)
  out.add('@'.code.toByte())
  val charactersPerLine = charactersFor(driver)
  for (line in text.lines()) {
    for (chunk in wrapLine(toPrinterAscii(line), charactersPerLine)) {
      chunk.encodeToByteArray().forEach { out.add(it) }
      out.add(EscPosBytes.LF)
    }
  }
  out.add(EscPosBytes.LF)
  out.add(EscPosBytes.LF)
  if (driver.openDrawer) {
    listOf(EscPosBytes.ESC, 'p'.code.toByte(), 0x00, 0x19, 0xFA.toByte()).forEach { out.add(it) }
  }
  if (driver.cut) {
    listOf(EscPosBytes.GS, 'V'.code.toByte(), 0x42, 0x00).forEach { out.add(it) }
  }
  return out.toByteArray()
}
