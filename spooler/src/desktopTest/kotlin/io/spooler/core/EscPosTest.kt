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

  private fun containsSubsequence(haystack: List<Byte>, needle: List<Byte>): Boolean {
    if (needle.isEmpty() || needle.size > haystack.size) return false
    for (start in 0..haystack.size - needle.size) {
      if (haystack.subList(start, start + needle.size) == needle) return true
    }
    return false
  }
}
