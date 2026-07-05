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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EscPosTest {
  private val cutBytes = listOf(0x1D.toByte(), 'V'.code.toByte(), 0x42.toByte(), 0x00.toByte())
  private val drawerBytes =
    listOf(0x1B.toByte(), 'p'.code.toByte(), 0x00.toByte(), 0x19.toByte(), 0xFA.toByte())

  @Test
  fun `starts with init bytes`() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false))
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals('@'.code.toByte(), bytes[1])
  }

  @Test
  fun `each line is followed by a line feed`() {
    val bytes =
      buildEscPos("line one\nline two", EscPosDriver(cut = false, openDrawer = false)).toList()
    val lineOneEnd = "line one".encodeToByteArray().toList()
    val lineTwoEnd = "line two".encodeToByteArray().toList()
    assertTrue(containsSubsequence(bytes, lineOneEnd + listOf(0x0A.toByte())))
    assertTrue(containsSubsequence(bytes, lineTwoEnd + listOf(0x0A.toByte())))
  }

  @Test
  fun `includes cut command when cut is true`() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = true, openDrawer = false)).toList()
    assertTrue(containsSubsequence(bytes, cutBytes))
  }

  @Test
  fun `omits cut command when cut is false`() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false)).toList()
    assertFalse(containsSubsequence(bytes, cutBytes))
  }

  @Test
  fun `includes drawer kick command when openDrawer is true`() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = true)).toList()
    assertTrue(containsSubsequence(bytes, drawerBytes))
  }

  @Test
  fun `omits drawer kick command when openDrawer is false`() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false)).toList()
    assertFalse(containsSubsequence(bytes, drawerBytes))
  }

  @Test
  fun `wraps long lines to charactersPerLine`() {
    val line = "a".repeat(100)
    val bytes =
      buildEscPos(line, EscPosDriver(charactersPerLine = 32, cut = false, openDrawer = false))
    val text = bytes.toString(Charsets.US_ASCII)
    val chunks = Regex("a+").findAll(text).map { it.value }.toList()
    assertTrue(chunks.isNotEmpty())
    assertTrue(chunks.all { it.length <= 32 })
    assertEquals(100, chunks.sumOf { it.length })
  }

  @Test
  fun `short line is emitted once`() {
    val bytes =
      buildEscPos("hi", EscPosDriver(charactersPerLine = 32, cut = false, openDrawer = false))
    val text = bytes.toString(Charsets.US_ASCII)
    assertTrue(text.contains("hi"))
    assertFalse(text.contains("hi\nhi"))
  }

  @Test
  fun `non-ascii characters are replaced with question marks`() {
    val bytes = buildEscPos("Café", EscPosDriver()).toList()
    assertFalse(bytes.any { it.toInt() and 0xFF > 0x7F })
    val text = bytes.toByteArray().toString(Charsets.US_ASCII)
    assertTrue(text.contains("Caf?"))
  }

  private fun containsSubsequence(haystack: List<Byte>, needle: List<Byte>): Boolean {
    if (needle.isEmpty() || needle.size > haystack.size) return false
    for (start in 0..haystack.size - needle.size) {
      if (haystack.subList(start, start + needle.size) == needle) return true
    }
    return false
  }
}
