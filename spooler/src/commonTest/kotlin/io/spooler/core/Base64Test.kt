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

class Base64Test {
  private fun ascii(s: String) = ByteArray(s.length) { s[it].code.toByte() }

  @Test
  fun knownVectors() {
    assertEquals("", Base64.encode(ascii("")))
    assertEquals("TQ==", Base64.encode(ascii("M")))
    assertEquals("TWE=", Base64.encode(ascii("Ma")))
    assertEquals("TWFu", Base64.encode(ascii("Man")))
    assertEquals("aGVsbG8=", Base64.encode(ascii("hello")))
  }

  @Test
  fun encodesBinaryBytes() {
    val bytes = byteArrayOf(0, 1, 2, (-1).toByte(), (-2).toByte(), 127, (-128).toByte())
    assertEquals("AAEC//5/gA==", Base64.encode(bytes))
  }
}
