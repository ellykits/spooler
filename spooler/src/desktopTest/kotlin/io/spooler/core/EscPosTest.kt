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

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EscPosTest {
  private val cutBytes = listOf(0x1D.toByte(), 'V'.code.toByte(), 0x42.toByte(), 0x00.toByte())
  private val drawerBytes =
    listOf(0x1B.toByte(), 'p'.code.toByte(), 0x00.toByte(), 0x19.toByte(), 0xFA.toByte())

  @Test
  fun startsWithInitBytes() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false))
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals('@'.code.toByte(), bytes[1])
  }

  @Test
  fun eachLineIsFollowedByALineFeed() {
    val bytes =
      buildEscPos("line one\nline two", EscPosDriver(cut = false, openDrawer = false)).toList()
    val lineOneEnd = "line one".encodeToByteArray().toList()
    val lineTwoEnd = "line two".encodeToByteArray().toList()
    assertTrue(containsSubsequence(bytes, lineOneEnd + listOf(0x0A.toByte())))
    assertTrue(containsSubsequence(bytes, lineTwoEnd + listOf(0x0A.toByte())))
  }

  @Test
  fun includesCutCommandWhenCutIsTrue() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = true, openDrawer = false)).toList()
    assertTrue(containsSubsequence(bytes, cutBytes))
  }

  @Test
  fun omitsCutCommandWhenCutIsFalse() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false)).toList()
    assertFalse(containsSubsequence(bytes, cutBytes))
  }

  @Test
  fun includesDrawerKickCommandWhenOpenDrawerIsTrue() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = true)).toList()
    assertTrue(containsSubsequence(bytes, drawerBytes))
  }

  @Test
  fun omitsDrawerKickCommandWhenOpenDrawerIsFalse() {
    val bytes = buildEscPos("hello", EscPosDriver(cut = false, openDrawer = false)).toList()
    assertFalse(containsSubsequence(bytes, drawerBytes))
  }

  @Test
  fun wrapsLongLinesToCharactersPerLine() {
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
  fun shortLineIsEmittedOnce() {
    val bytes =
      buildEscPos("hi", EscPosDriver(charactersPerLine = 32, cut = false, openDrawer = false))
    val text = bytes.toString(Charsets.US_ASCII)
    assertTrue(text.contains("hi"))
    assertFalse(text.contains("hi\nhi"))
  }

  @Test
  fun selectsTheCodePageRightAfterInit() {
    val bytes = buildEscPos("hi", EscPosDriver(codePage = EscPosCodePage.PC858)).toList()
    assertEquals(listOf(0x1B, '@'.code, 0x1B, 't'.code, 19).map { it.toByte() }, bytes.take(5))
  }

  @Test
  fun accentedLettersPrintFromTheCodePage() {
    val bytes = buildEscPos("Café", EscPosDriver()).toList()
    assertTrue(containsSubsequence(bytes, "Caf".encodeToByteArray().toList() + 0x82.toByte()))
  }

  @Test
  fun theEuroSignPrintsWhereThePageHasIt() {
    val pc858 = buildEscPos("€5", EscPosDriver(codePage = EscPosCodePage.PC858)).toList()
    assertTrue(containsSubsequence(pc858, listOf(0xD5.toByte(), '5'.code.toByte())))
    val pc437 = buildEscPos("€5", EscPosDriver()).toString(Charsets.US_ASCII)
    assertTrue(pc437.contains("EUR5"), pc437)
  }

  @Test
  fun lettersThePageLacksPrintInTheirPlainForm() {
    val bytes = buildEscPos("São Tomé", EscPosDriver()).toList()
    assertTrue(containsSubsequence(bytes, "Sao Tom".encodeToByteArray().toList() + 0x82.toByte()))
  }

  @Test
  fun charactersWithNoPlainFormPrintAsQuestionMarks() {
    val text = buildEscPos("茶", EscPosDriver()).toString(Charsets.US_ASCII)
    assertTrue(text.contains("?"), text)
  }

  @Test
  fun codePagesMatchTheJdksCharsets() {
    val upper = ByteArray(128) { (0x80 + it).toByte() }
    for ((page, charset) in
      listOf(EscPosCodePage.PC437 to "IBM437", EscPosCodePage.PC858 to "IBM00858")) {
      val text = String(upper, Charset.forName(charset))
      val bytes = buildEscPos(text, EscPosDriver(charactersPerLine = 0, codePage = page))
      assertEquals(upper.toList(), bytes.toList().subList(5, 5 + 128), "$page against $charset")
    }
  }

  private fun containsSubsequence(haystack: List<Byte>, needle: List<Byte>): Boolean {
    if (needle.isEmpty() || needle.size > haystack.size) return false
    for (start in 0..haystack.size - needle.size) {
      if (haystack.subList(start, start + needle.size) == needle) return true
    }
    return false
  }

  @Test
  fun stripsHtmlDownToPrintableText() {
    val text =
      htmlToText("<html><head><style>x{}</style></head><body><p>Total</p><p>100</p></body></html>")

    assertEquals("Total\n100", text)
  }

  @Test
  fun derivesCharactersPerLineFromPaperWidthWhenNotSet() {
    val line = "X".repeat(70)

    val narrow = buildEscPos(line, EscPosDriver(paperWidthMm = 58))
    val narrowChunks = Regex("X+").findAll(narrow.decodeToString()).map { it.value }.toList()
    assertEquals(32, narrowChunks.first().length, "58mm paper should wrap at 32 characters")
    assertTrue(narrowChunks.all { it.length <= 32 })

    val wide = buildEscPos(line, EscPosDriver(paperWidthMm = 80))
    val wideChunks = Regex("X+").findAll(wide.decodeToString()).map { it.value }.toList()
    assertEquals(48, wideChunks.first().length, "80mm paper should wrap at 48 characters")
    assertTrue(wideChunks.all { it.length <= 48 })
  }

  @Test
  fun explicitCharactersPerLineOverridesPaperWidthDerivation() {
    val line = "X".repeat(70)

    val overridden = buildEscPos(line, EscPosDriver(paperWidthMm = 80, charactersPerLine = 20))
    val overriddenChunks =
      Regex("X+").findAll(overridden.decodeToString()).map { it.value }.toList()
    assertEquals(20, overriddenChunks.first().length, "explicit charactersPerLine should win")

    val neverWraps = buildEscPos(line, EscPosDriver(paperWidthMm = 80, charactersPerLine = 0))
    val neverWrapsChunks =
      Regex("X+").findAll(neverWraps.decodeToString()).map { it.value }.toList()
    assertEquals(listOf(line), neverWrapsChunks, "explicit 0 must still mean never wrap")
  }
}
