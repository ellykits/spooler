package io.spooler.core

import kotlin.test.Test
import kotlin.test.assertEquals

class KmpBase64Test {
  private fun ascii(s: String) = ByteArray(s.length) { s[it].code.toByte() }

  @Test
  fun knownVectors() {
    assertEquals("", KmpBase64.encode(ascii("")))
    assertEquals("TQ==", KmpBase64.encode(ascii("M")))
    assertEquals("TWE=", KmpBase64.encode(ascii("Ma")))
    assertEquals("TWFu", KmpBase64.encode(ascii("Man")))
    assertEquals("aGVsbG8=", KmpBase64.encode(ascii("hello")))
  }

  @Test
  fun encodesBinaryBytes() {
    val bytes = byteArrayOf(0, 1, 2, (-1).toByte(), (-2).toByte(), 127, (-128).toByte())
    assertEquals("AAEC//5/gA==", KmpBase64.encode(bytes))
  }
}
